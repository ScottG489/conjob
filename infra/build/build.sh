#!/usr/bin/env bash

readonly IMAGE_NAME='scottg489/docker-build-push:latest'
readonly ID_RSA=$1
readonly DOCKER_CONFIG=$2
readonly GIT_BRANCH=${GITHUB_HEAD_REF:-$GITHUB_REF_NAME}
readonly DOCKER_IMAGE_TAG=$([[ $GIT_BRANCH == "master" ]] && echo -n "latest" || sed 's/[^a-zA-Z0-9]/-/g' <<< "$GIT_BRANCH")
readonly DOCKER_IMAGE_NAME="scottg489/conjob-build:$DOCKER_IMAGE_TAG"

read -r -d '' JSON_BODY <<- EOM
  {
  "ID_RSA": "$ID_RSA",
  "DOCKER_CONFIG": "$DOCKER_CONFIG",
  "GIT_REPO_URL": "$GIT_REPO_URL",
  "GIT_BRANCH": "$GIT_BRANCH",
  "RELATIVE_SUB_DIR": "$RELATIVE_SUB_DIR",
  "DOCKER_IMAGE_NAME": "$DOCKER_IMAGE_NAME"
  }
EOM

curl -v -sS --insecure -w '\n%{http_code}' \
  --data-binary "$JSON_BODY" \
  "https://alt.conjob.io:18080/job/run?image=$IMAGE_NAME" \
  | tee /tmp/foo \
  | sed '$d' && \
  [ "$(tail -1 /tmp/foo)" -eq 200 ]
