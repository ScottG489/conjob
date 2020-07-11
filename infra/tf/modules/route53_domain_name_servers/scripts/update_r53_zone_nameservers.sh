#!/bin/bash
set -ex
declare -r DOMAIN_NAME=$1
declare -r NAME_SERVER_1=$2
declare -r NAME_SERVER_2=$3
declare -r NAME_SERVER_3=$4
declare -r NAME_SERVER_4=$5

## Terraform can't manage domains. This gets the nameservers off the hosted zone and sets them as the nameservers for the domain
aws --region us-east-1 route53domains update-domain-nameservers \
  --domain-name "$DOMAIN_NAME" \
  --nameservers Name="$NAME_SERVER_1" Name="$NAME_SERVER_2" Name="$NAME_SERVER_3" Name="$NAME_SERVER_4"
