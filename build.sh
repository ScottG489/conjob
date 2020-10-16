curl -v -sS -w '%{http_code}' \
  --data-binary '{"ID_RSA": "'"$1"'", "DOCKER_CONFIG": "'"$2"'", "AWS_CREDENTIALS": "'"$3"'", "MAIN_KEY_PAIR": "'"$4"'"}' \
  'http://alt.simple-ci.com/build?image=scottg489/docker-ci-prototype-build:latest' \
  | tee /tmp/foo \
  | sed '$d' && \
  [ "$(tail -1 /tmp/foo)" -eq 200 ]
