#!/bin/bash
set -e

source /opt/build/build_functions.sh

# conjob stops a job that runs past its time limit with docker stop: a SIGTERM, then a SIGKILL once
# the grace period is up. This script is PID 1, which ignores any signal it doesn't trap, so without
# this the SIGTERM did nothing and run-test.sh never got to destroy its test env.
_TERMINATING=''
_TEST_PID=''
on_sigterm() {
  _TERMINATING=1
  # Outside the tests, carry on. Bash only runs this once the step in progress has finished, and a
  # deploy that's underway is better finished than cut off halfway.
  [[ -n $_TEST_PID ]] || return 0
  # Signal every other process, so run-test.sh stops its current step and destroys the test env,
  # and wait for that.
  kill -TERM -1 2> /dev/null || true
  wait "$_TEST_PID" || true
  exit 143
}
trap on_sigterm TERM

set +x
setup_credentials "$1"
set -x

# Start the docker daemon. This is necessary when using the sysbox-runc container runtime rather than mounting docker.sock
dockerd > /var/log/dockerd.log 2>&1 &
wait_for_dockerd $!

declare -r _PROJECT_NAME='conjob'
declare -r _GIT_REPO='git@github.com:ScottG489/conjob.git'
declare -r _TFSTATE_BUCKET_NAME='tfstate-conjob'
declare -r _RUN_TASK=$(jq -r .RUN_TASK <<< "$1")
declare -r _GIT_BRANCH=$(jq -r .GIT_BRANCH <<< "$1")
declare -r _DOCKER_IMAGE_TAG=$(jq -r .DOCKER_IMAGE_TAG <<< "$1")

[ -d "$_PROJECT_NAME" ] || git clone --branch $_GIT_BRANCH $_GIT_REPO
cp -r $_PROJECT_NAME "$_PROJECT_NAME"_build
cd "$_PROJECT_NAME"_build

build_test
push_application $_DOCKER_IMAGE_TAG

# Once the time limit is up, there's no time to create a test env, test it and destroy it.
[[ -z $_TERMINATING ]] || exit 143

set +x
# In the background, so that a SIGTERM is handled during the tests instead of after them.
/opt/build/run-test.sh "$1" $_DOCKER_IMAGE_TAG &
_TEST_PID=$!
set -x
wait "$_TEST_PID"
_TEST_PID=''

[ "$_RUN_TASK" != "deploy" ] && exit 0

push_application "latest"

tf_backend_init $_TFSTATE_BUCKET_NAME "infra/tf"

tf_apply "infra/tf"

set +x
setup_application_configuration "$1"
set -x

export _DISABLE_SNI_HOST_CHECK='false'
ansible_deploy "infra/tf" $_DOCKER_IMAGE_TAG
