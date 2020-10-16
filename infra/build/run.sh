#!/bin/bash
set -e

source /opt/build/build_functions.sh

set +x
setup_credentials "$1"
set -x

declare -r _PROJECT_NAME='docker-ci-prototype'
declare -r _GIT_REPO='git@github.com:ScottG489/docker-ci-prototype.git'
declare -r _TFSTATE_BUCKET_NAME='tfstate-docker-ci-prototype'

git clone $_GIT_REPO
cd $_PROJECT_NAME

build_push_application

/opt/build/run-test.sh

tf_backend_init $_TFSTATE_BUCKET_NAME "infra/tf"

tf_apply "infra/tf"

ansible_deploy "infra/tf"
