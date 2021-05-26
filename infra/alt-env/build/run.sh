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
declare -r _TFSTATE_BUCKET_NAME='tfstate-alt-conjob'

git clone $_GIT_REPO
cd $_PROJECT_NAME

# We don't really need to build the application. The latest changes should have been pushed to docker hub and we aren't trying to deploy with local changes
#build_push_application

tf_backend_init $_TFSTATE_BUCKET_NAME "infra/alt-env/tf"

tf_apply "infra/alt-env/tf"

set +x
setup_application_configuration "$1"
set -x

ansible_deploy "infra/alt-env/tf"
