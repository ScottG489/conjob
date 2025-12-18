#!/usr/bin/env bash

readonly GIT_BRANCH=${GITHUB_HEAD_REF:-$GITHUB_REF_NAME}
readonly DOCKER_IMAGE_TAG=$([[ $GIT_BRANCH == "master" ]] && echo -n "latest" || sed 's/[^a-zA-Z0-9]/-/g' <<< "$GIT_BRANCH")
readonly IMAGE_NAME="scottg489/conjob-build:$DOCKER_IMAGE_TAG"
readonly RUN_TASK=$1
readonly ID_RSA=$2
readonly DOCKER_CONFIG=$3
readonly AWS_CREDENTIALS=$4
readonly MAIN_KEY_PAIR=$5
readonly ADMIN_USERNAME=$6
readonly ADMIN_PASSWORD=$7
readonly DOCKER_USERNAME=$8
readonly DOCKER_PASSWORD=$9

read -r -d '' JSON_BODY <<- EOM
  {
  "RUN_TASK": "$RUN_TASK",
  "GIT_BRANCH": "$GIT_BRANCH",
  "DOCKER_IMAGE_TAG": "$DOCKER_IMAGE_TAG",
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

curl -v --insecure -sS -w '\n%{http_code}' \
  --data-binary "$JSON_BODY" \
  "https://alt.conjob.io/job/run?image=$IMAGE_NAME" \
  | tee /tmp/foo \
  | sed '$d' && \
  [ "$(tail -1 /tmp/foo)" -eq 200 ]
