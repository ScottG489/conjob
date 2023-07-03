#!/bin/bash
set -e

source /opt/build/build_functions.sh

set +x
setup_credentials "$1"
set -x

declare -r _PROJECT_NAME='conjob'
declare -r _TFSTATE_BUCKET_NAME='tfstate-alt-conjob'

cp -r $_PROJECT_NAME "$_PROJECT_NAME"_build
cd "$_PROJECT_NAME"_build


tf_backend_init $_TFSTATE_BUCKET_NAME "infra/alt-env/tf"

tf_apply "infra/alt-env/tf"

set +x
setup_application_configuration "$1"
set -x

ansible_deploy "infra/alt-env/tf"
