curl -v -sS -w '%{http_code}' \
  --data-binary '{"ID_RSA": "'"$1"'", "DOCKER_CONFIG": "'"$2"'", "GIT_REPO_URL": "'"$GIT_REPO_URL"'", "RELATIVE_SUB_DIR": "'"$RELATIVE_SUB_DIR"'", "DOCKER_IMAGE_NAME": "'"$DOCKER_IMAGE_NAME"'"}' \
  'http://simple-ci.com/build?image=scottg489/docker-build-push:latest' \
  | tee /tmp/foo \
  | sed '$d' && \
  [ "$(tail -1 /tmp/foo)" -eq 200 ]
