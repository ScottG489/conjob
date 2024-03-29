#!/bin/bash
set -e

get_git_root_dir() {
  echo -n "$(git rev-parse --show-toplevel)"
}

setup_credentials() {
  set +x
  local ID_RSA_CONTENTS
  local MAINKEYPAIR_CONTENTS
  local AWS_CREDENTIALS_CONTENTS
  local DOCKER_CONFIG_CONTENTS

  readonly ID_RSA_CONTENTS=$(echo -n "$1" | jq -r .ID_RSA | base64 --decode)
  readonly MAINKEYPAIR_CONTENTS=$(echo -n "$1" | jq -r .MAIN_KEY_PAIR | base64 --decode)
  readonly AWS_CREDENTIALS_CONTENTS=$(echo -n "$1" | jq -r .AWS_CREDENTIALS | base64 --decode)
  readonly DOCKER_CONFIG_CONTENTS=$(echo -n "$1" | jq -r .DOCKER_CONFIG | base64 --decode)
  [[ -n $ID_RSA_CONTENTS ]]
  [[ -n $MAINKEYPAIR_CONTENTS ]]
  [[ -n $AWS_CREDENTIALS_CONTENTS ]]
  [[ -n $DOCKER_CONFIG_CONTENTS ]]

  printf -- "$ID_RSA_CONTENTS" >/root/.ssh/id_rsa
  printf -- "$MAINKEYPAIR_CONTENTS" >/root/.ssh/mainkeypair.pem
  printf -- "$AWS_CREDENTIALS_CONTENTS" >/root/.aws/credentials
  printf -- "$DOCKER_CONFIG_CONTENTS" >/root/.docker/config.json

  chmod 400 /root/.ssh/id_rsa
  chmod 400 /root/.ssh/mainkeypair.pem
}

build_push_application() {
  local ROOT_DIR
  readonly ROOT_DIR=$(get_git_root_dir)
  cd "$ROOT_DIR"

  ./gradlew --info build unitTest integrationTest install

  docker build -t scottg489/conjob:latest .
  docker push scottg489/conjob:latest
}

tf_backend_init() {
  local ROOT_DIR
  local TFSTATE_BACKEND_BUCKET_NAME
  local RELATIVE_PATH_TO_TF_DIR

  readonly ROOT_DIR=$(get_git_root_dir)
  readonly TFSTATE_BACKEND_BUCKET_NAME=$1
  readonly RELATIVE_PATH_TO_TF_DIR=$2

  cd "$ROOT_DIR/$RELATIVE_PATH_TO_TF_DIR/backend-init"

  # Initialize terraform backend on first deploy
  aws s3 ls "$TFSTATE_BACKEND_BUCKET_NAME" &&
    (terraform init &&
      terraform import aws_s3_bucket.backend_bucket "$TFSTATE_BACKEND_BUCKET_NAME")

  terraform init
  terraform plan
  terraform apply --auto-approve
}

tf_apply() {
  local ROOT_DIR
  local RELATIVE_PATH_TO_TF_DIR
  local HOSTED_ZONE_DNS_NAME

  readonly ROOT_DIR=$(get_git_root_dir)
  readonly RELATIVE_PATH_TO_TF_DIR=$1

  cd "$ROOT_DIR/$RELATIVE_PATH_TO_TF_DIR"

  terraform init

  # We need to import the zone because it is a shared resource and may already exist
  readonly HOSTED_ZONE_DNS_NAME=$(echo '"${var.second_level_domain_name}.${var.top_level_domain_name}"' | terraform console | tr -d \")
  [[ -n $HOSTED_ZONE_DNS_NAME ]]
  readonly EXISTING_ZONE_ID=$(aws route53 list-hosted-zones-by-name |
    jq --raw-output --arg name "$HOSTED_ZONE_DNS_NAME" '.HostedZones | .[] | select(.Name == "\($name).") | .Id')
  terraform import module.conjob.aws_route53_zone.r53_zone "$EXISTING_ZONE_ID" || true

  terraform plan
  terraform apply --auto-approve
}

setup_application_configuration() {
  set +x
  [[ -n $1 ]]
  local ROOT_DIR
  local BUILD_SCRIPT_JSON_INPUT

  local DOCKER_USERNAME
  local DOCKER_PASSWORD
  local ADMIN_USERNAME
  local ADMIN_PASSWORD

  readonly ROOT_DIR=$(get_git_root_dir)
  readonly BUILD_SCRIPT_JSON_INPUT=$1

  readonly DOCKER_USERNAME=$(echo -n "$BUILD_SCRIPT_JSON_INPUT" | jq -r .DOCKER_USERNAME)
  [[ -n $DOCKER_USERNAME ]]
  readonly DOCKER_PASSWORD=$(echo -n "$BUILD_SCRIPT_JSON_INPUT" | jq -r .DOCKER_PASSWORD)
  [[ -n $DOCKER_PASSWORD ]]
  readonly ADMIN_USERNAME=$(echo -n "$BUILD_SCRIPT_JSON_INPUT" | jq -r .ADMIN_USERNAME)
  [[ -n $ADMIN_USERNAME ]]
  readonly ADMIN_PASSWORD=$(echo -n "$BUILD_SCRIPT_JSON_INPUT" | jq -r .ADMIN_PASSWORD)
  [[ -n $ADMIN_PASSWORD ]]

  # These are used in the ansible playbook
  export _DOCKER_USERNAME=$DOCKER_USERNAME
  export _DOCKER_PASSWORD=$DOCKER_PASSWORD
  export _ADMIN_USERNAME=$ADMIN_USERNAME
  export _ADMIN_PASSWORD=$ADMIN_PASSWORD
  export _CONTAINER_RUNTIME=sysbox_runc
}

ansible_deploy() {
  local ROOT_DIR
  local RELATIVE_PATH_TO_TF_DIR
  local PUBLIC_IP
  local KEYSTORE_TEMP_FILE
  local KEYSTORE_PASSWORD
  local _KEYSTORE_PASSWORD

  readonly ROOT_DIR=$(get_git_root_dir)
  readonly RELATIVE_PATH_TO_TF_DIR=$1

  cd "$ROOT_DIR/$RELATIVE_PATH_TO_TF_DIR"

  readonly PUBLIC_IP=$(terraform show --json | jq --raw-output '.values.outputs.instance_public_ip.value')
  [[ -n $PUBLIC_IP ]]
  readonly KEYSTORE_TEMP_FILE=$(mktemp)
  terraform show --json | jq --raw-output '.values.outputs.certificate_p12.value' | base64 --decode > "$KEYSTORE_TEMP_FILE"
  set +x
  readonly KEYSTORE_PASSWORD=$(terraform show --json | jq --raw-output '.values.outputs.keystore_password.value')
  [[ -n $KEYSTORE_PASSWORD ]]
  set -x

  cd "$ROOT_DIR/infra/ansible"

  mkdir -p files
  set +x
  printf -- "$KEYSTORE_PASSWORD"\\n"$KEYSTORE_PASSWORD"\\n"$KEYSTORE_PASSWORD" |
    keytool -v -importkeystore -srckeystore "$KEYSTORE_TEMP_FILE" -srcstoretype PKCS12 -destkeystore files/keystore.p12 -deststoretype PKCS12
  set -x
  [[ -f files/keystore.p12 ]]

  set +x
  export _KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD
  set -x
  ansible-playbook -v -u ubuntu -e ansible_ssh_private_key_file=/root/.ssh/mainkeypair.pem --inventory "$PUBLIC_IP", master-playbook.yml
  rm files/keystore.p12
}

run_tests() {
  local ROOT_DIR
  local RELATIVE_PATH_TO_TF_DIR
  local PUBLIC_IP

  readonly RELATIVE_PATH_TO_TF_DIR=$1
  readonly ROOT_DIR=$(get_git_root_dir)

  cd "$ROOT_DIR/$RELATIVE_PATH_TO_TF_DIR"
  readonly PUBLIC_IP=$(terraform show --json | jq --raw-output '.values.outputs.instance_public_ip.value')
  [[ -n $PUBLIC_IP ]]

  cd "$ROOT_DIR"

  # Acceptance test configuration
  echo "baseUri=https://${PUBLIC_IP}" >"$ROOT_DIR/src/test/acceptance/resource/config.properties"
  echo "adminBaseUri=http://${PUBLIC_IP}:8081" >>"$ROOT_DIR/src/test/acceptance/resource/config.properties"
  echo "adminUsername=${_ADMIN_USERNAME}" >>"$ROOT_DIR/src/test/acceptance/resource/config.properties"
  echo "adminPassword=${_ADMIN_PASSWORD}" >>"$ROOT_DIR/src/test/acceptance/resource/config.properties"
  # Performance test configuration
  echo "baseUri=https://${PUBLIC_IP}" >"$ROOT_DIR/src/test/performance/resources/config.properties"
  echo "adminBaseUri=http://${PUBLIC_IP}:8081" >>"$ROOT_DIR/src/test/performance/resources/config.properties"

  ./gradlew --info acceptanceTest performanceTest
}
