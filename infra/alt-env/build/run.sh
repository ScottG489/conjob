#!/bin/bash
set -e

source /opt/build/build_functions.sh

set +x
setup_credentials "$1"
set -x

declare -r _PROJECT_NAME='docker-ci-prototype'
declare -r _GIT_REPO='git@github.com:ScottG489/docker-ci-prototype.git'
declare -r _TFSTATE_BUCKET_NAME='tfstate-alt-docker-ci-prototype'

# Since the alt-env is never meant to be run other than locally we'll always be mounting the local repo
#git clone $_GIT_REPO
cd $_PROJECT_NAME

build_push_application

tf_backend_init $_TFSTATE_BUCKET_NAME "infra/alt-env/tf"

tf_apply "infra/alt-env/tf"

ansible_deploy "infra/alt-env/tf"
