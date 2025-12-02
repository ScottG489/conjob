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

[ -d "$_PROJECT_NAME" ] || git clone $_GIT_REPO
cp -r $_PROJECT_NAME "$_PROJECT_NAME"_build
cd "$_PROJECT_NAME"_build

build_push_application

set +x
/opt/build/run-test.sh "$1"
set -x

tf_backend_init $_TFSTATE_BUCKET_NAME "infra/tf"

tf_apply "infra/tf"

set +x
setup_application_configuration "$1"
set -x

ansible_deploy "infra/tf"
