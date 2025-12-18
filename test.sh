#!/bin/bash
set -ex

trap cleanup EXIT
cleanup() {
  # The local fs is mounted into the container and as such any files it writes will have their permissions changed.
  #   This will change the permissions back and clean up other files we don't want hanging around.
  sudo chown -R "$(whoami)":"$(whoami)" .
  find . -name '*terraform.tfstate*' -exec rm {} \;
  find . -name '.terraform' -type d -prune -exec rm -rf {} \;
}

declare ID_RSA_CONTENTS_BASE64
declare AWS_CREDENTIALS_CONTENTS_BASE64
declare MAINKEYPAIR_CONTENTS_BASE64
declare DOCKER_CONFIG_CONTENTS_BASE64

# Change the location of these files based on where they are on your system
set +x
ID_RSA_CONTENTS_BASE64=$(base64 ~/.ssh/id_rsa | tr -d '\n') ;
AWS_CREDENTIALS_CONTENTS_BASE64=$(base64 ~/.aws/credentials | tr -d '\n') ;
MAINKEYPAIR_CONTENTS_BASE64=$(base64 ~/.ssh/mainkeypair.pem | tr -d '\n') ;
DOCKER_CONFIG_CONTENTS_BASE64=$(base64 ~/.docker/config.json | tr -d '\n') ;
[[ -n $ID_RSA_CONTENTS_BASE64 ]]
[[ -n $AWS_CREDENTIALS_CONTENTS_BASE64 ]]
[[ -n $MAINKEYPAIR_CONTENTS_BASE64 ]]
[[ -n $DOCKER_CONFIG_CONTENTS_BASE64 ]]
set -x

# The local fs is mounted into the container and as such any files it writes will have their permissions changed.
#   This will change the permissions back and clean up other files we don't want hanging around.
sudo chown -R "$(whoami)":"$(whoami)" .
find . -name '*terraform.tfstate*' -exec rm {} \;
find . -name '.terraform' -type d -prune -exec rm -rf {} \;

docker build infra/build -t conjob-build-test && \
  docker run -it \
  --runtime=sysbox-runc \
  --volume "$PWD:/opt/build/conjob" \
  conjob-build-test '{"RUN_TASK": "test", "DOCKER_IMAGE_TAG": "local_test", "ID_RSA": "'"$ID_RSA_CONTENTS_BASE64"'", "AWS_CREDENTIALS": "'"$AWS_CREDENTIALS_CONTENTS_BASE64"'", "MAIN_KEY_PAIR": "'"$MAINKEYPAIR_CONTENTS_BASE64"'", "DOCKER_CONFIG": "'"$DOCKER_CONFIG_CONTENTS_BASE64"'"}'
