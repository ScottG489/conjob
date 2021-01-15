#!/usr/bin/env bash

readonly IMAGE_NAME='scottg489/docker-build-push:latest'
readonly ID_RSA=$1
readonly DOCKER_CONFIG=$2

read -r -d '' JSON_BODY <<- EOM
  {
  "ID_RSA": "$ID_RSA",
  "DOCKER_CONFIG": "$DOCKER_CONFIG",
  "GIT_REPO_URL": "$GIT_REPO_URL",
  "RELATIVE_SUB_DIR": "$RELATIVE_SUB_DIR",
  "DOCKER_IMAGE_NAME": "$DOCKER_IMAGE_NAME"
  }
EOM

curl -v -sS -w '\n%{http_code}' \
  --data-binary "$JSON_BODY" \
  "http://alt.conjob.io/job/run?image=$IMAGE_NAME" \
  | tee /tmp/foo \
  | sed '$d' && \
  [ "$(tail -1 /tmp/foo)" -eq 200 ]
