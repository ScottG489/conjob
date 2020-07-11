#!/bin/bash
set -ex

declare -r SPOT_INSTANCE_ID=$1
declare -r TAG_NAME=$2

aws ec2 create-tags --resources "$SPOT_INSTANCE_ID" --tags Key=Name,Value="$TAG_NAME"