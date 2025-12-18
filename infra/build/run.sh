#!/bin/bash
set -e

source /opt/build/build_functions.sh

set +x
setup_credentials "$1"
set -x

# Start the docker daemon. This is necessary when using the sysbox-runc container runtime rather than mounting docker.sock
dockerd > /var/log/dockerd.log 2>&1 &
sleep 3

declare -r _PROJECT_NAME='conjob'
declare -r _GIT_REPO='git@github.com:ScottG489/conjob.git'
declare -r _TFSTATE_BUCKET_NAME='tfstate-conjob'
declare -r _RUN_TASK=$(jq -r .RUN_TASK <<< "$1")
declare -r _GIT_BRANCH=$(jq -r .GIT_BRANCH <<< "$1")
declare -r _DOCKER_IMAGE_TAG=$(jq -r .DOCKER_IMAGE_TAG <<< "$1")

if [ ! -d "$_PROJECT_NAME" ]; then
  git clone --branch $_GIT_BRANCH $_GIT_REPO
fi
cp -r $_PROJECT_NAME "$_PROJECT_NAME"_build
cd "$_PROJECT_NAME"_build

build_test
push_application $_DOCKER_IMAGE_TAG

set +x
/opt/build/run-test.sh "$1" $_DOCKER_IMAGE_TAG
set -x

[ "$_RUN_TASK" != "deploy" ] && exit 0

push_application "latest"

tf_backend_init $_TFSTATE_BUCKET_NAME "infra/tf"

tf_apply "infra/tf"

set +x
setup_application_configuration "$1"
set -x

ansible_deploy "infra/tf" $_DOCKER_IMAGE_TAG
