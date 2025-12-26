#!/bin/bash
set -ex

source /opt/build/build_functions.sh

trap cleanup EXIT
cleanup() {
  cd "$(git rev-parse --show-toplevel)/infra/tf/test-env"
  terraform destroy --auto-approve
}

declare -r _DOCKER_IMAGE_TAG=$2

tf_apply "infra/tf/test-env"

set +x
setup_application_configuration "$1"
set -x

export _DISABLE_SNI_HOST_CHECK='true'
ansible_deploy "infra/tf/test-env" $_DOCKER_IMAGE_TAG
unset _DISABLE_SNI_HOST_CHECK

run_tests "infra/tf/test-env"
