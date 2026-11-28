#!/bin/bash
set -ex

source /opt/build/build_functions.sh

trap cleanup EXIT
# run.sh passes on conjob's SIGTERM when the job runs out of time, which stops the step in progress
# too. With the SIGTERM trapped, bash waits for that step to exit before it exits itself, so the
# destroy doesn't start while, say, terraform apply is still shutting down.
trap 'exit 143' TERM
cleanup() {
  # So that a SIGTERM can't stop the destroy partway. The commands below inherit this.
  trap '' TERM
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
