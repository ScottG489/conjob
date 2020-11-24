#!/usr/bin/env bash

readonly IMAGE_NAME='scottg489/conjob-build:latest'
readonly ID_RSA=$1
readonly DOCKER_CONFIG=$2
readonly AWS_CREDENTIALS=$3
readonly MAIN_KEY_PAIR=$4
readonly ADMIN_USERNAME=$5
readonly ADMIN_PASSWORD=$6
readonly DOCKER_USERNAME=$7
readonly DOCKER_PASSWORD=$8

read -r -d '' JSON_BODY <<- EOM
  {
  "ID_RSA": "$ID_RSA",
  "DOCKER_CONFIG": "$DOCKER_CONFIG",
  "AWS_CREDENTIALS": "$AWS_CREDENTIALS",
  "MAIN_KEY_PAIR": "$MAIN_KEY_PAIR",
  "DOCKER_USERNAME": "$DOCKER_USERNAME",
  "DOCKER_PASSWORD": "$DOCKER_PASSWORD",
  "ADMIN_USERNAME": "$ADMIN_USERNAME",
  "ADMIN_PASSWORD": "$ADMIN_PASSWORD"
  }
EOM

curl -v -sS -w '%{http_code}' \
  --data-binary "$JSON_BODY" \
  "http://alt.conjob.io/job/run?image=$IMAGE_NAME" \
  | tee /tmp/foo \
  | sed '$d' && \
  [ "$(tail -1 /tmp/foo)" -eq 200 ]
