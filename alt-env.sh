#!/bin/bash
# Don't turn on xtrace (set -x) here: it would print the base64-encoded credentials that are
# passed to docker run.
set -e

declare ID_RSA_CONTENTS_BASE64
declare AWS_CREDENTIALS_CONTENTS_BASE64
declare DOCKER_CONFIG_CONTENTS_BASE64

# Change the location of these files based on where they are on your system
ID_RSA_CONTENTS_BASE64=$(base64 ~/.ssh/id_rsa | tr -d '\n') ;
AWS_CREDENTIALS_CONTENTS_BASE64=$(base64 ~/.aws/credentials | tr -d '\n') ;
DOCKER_CONFIG_CONTENTS_BASE64=$(base64 ~/.docker/config.json | tr -d '\n') ;
[[ -n $ID_RSA_CONTENTS_BASE64 ]]
[[ -n $AWS_CREDENTIALS_CONTENTS_BASE64 ]]
[[ -n $DOCKER_CONFIG_CONTENTS_BASE64 ]]

# Only use a terminal when there is one, so the script also runs without one.
declare -a TTY_FLAGS=()
[[ -t 0 && -t 1 ]] && TTY_FLAGS=(-it)

# The deploy works on its own copy of the mounted repo, so it doesn't change any files in it.
docker build infra/alt-env/build -t conjob-build-test
docker run "${TTY_FLAGS[@]}" --rm \
  --runtime=sysbox-runc \
  --volume "$PWD:/opt/build/conjob" \
  conjob-build-test '{"ID_RSA": "'"$ID_RSA_CONTENTS_BASE64"'", "AWS_CREDENTIALS": "'"$AWS_CREDENTIALS_CONTENTS_BASE64"'", "DOCKER_CONFIG": "'"$DOCKER_CONFIG_CONTENTS_BASE64"'"}'
