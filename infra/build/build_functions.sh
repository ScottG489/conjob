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

  readonly ID_RSA_CONTENTS=$(echo -n $1 | jq -r .ID_RSA | base64 --decode)
  readonly MAINKEYPAIR_CONTENTS=$(echo -n $1 | jq -r .MAIN_KEY_PAIR | base64 --decode)
  readonly AWS_CREDENTIALS_CONTENTS=$(echo -n $1 | jq -r .AWS_CREDENTIALS | base64 --decode)
  readonly DOCKER_CONFIG_CONTENTS=$(echo -n $1 | jq -r .DOCKER_CONFIG | base64 --decode)

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

  ./gradlew --info build unitTest install

  docker build -t scottg489/docker-ci-prototype:latest .
  docker push scottg489/docker-ci-prototype:latest
}

tf_backend_init() {
  local ROOT_DIR
  local TFSTATE_BACKEND_BUCKET_NAME

  readonly ROOT_DIR=$(get_git_root_dir)
  readonly TFSTATE_BACKEND_BUCKET_NAME=$1

  cd "$ROOT_DIR/infra/tf/backend-init"

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

  readonly ROOT_DIR=$(get_git_root_dir)
  readonly RELATIVE_PATH_TO_TF_DIR=$1

  cd "$ROOT_DIR/$RELATIVE_PATH_TO_TF_DIR"

  terraform init
  terraform plan
  terraform apply --auto-approve
}

ansible_deploy() {
  local ROOT_DIR
  local RELATIVE_PATH_TO_TF_DIR
  local PUBLIC_IP

  readonly ROOT_DIR=$(get_git_root_dir)
  readonly RELATIVE_PATH_TO_TF_DIR=$1

  cd "$ROOT_DIR/$RELATIVE_PATH_TO_TF_DIR"

  readonly PUBLIC_IP=$(terraform show --json | jq --raw-output '.values.outputs.instance_public_ip.value')
  [[ -n $PUBLIC_IP ]]

  cd "$ROOT_DIR/infra/ansible"
  ansible-playbook -v -u ubuntu -e ansible_ssh_private_key_file=/root/.ssh/mainkeypair.pem --inventory "$PUBLIC_IP", master-playbook.yml
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

  echo "baseUri=http://${PUBLIC_IP}:80" >"$ROOT_DIR/src/test/acceptance/resource/config.properties"
  echo "adminBaseUri=http://${PUBLIC_IP}:8081" >>"$ROOT_DIR/src/test/acceptance/resource/config.properties"

  ./gradlew --info acceptanceTest
}
