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

set +x
setup_application_configuration "$1"
set -x

export _DISABLE_SNI_HOST_CHECK='true'
ansible_deploy
