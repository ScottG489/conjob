curl -v -sS -w '%{http_code}' \
  --data-binary '{"ID_RSA": "'"$1"'", "DOCKER_CONFIG": "'"$2"'", "AWS_CREDENTIALS": "'"$3"'", "MAIN_KEY_PAIR": "'"$4"'"}' \
  'http://ec2-34-211-180-243.us-west-2.compute.amazonaws.com/build?image=scottg489/docker-ci-prototype-build:latest' \
  | tee /tmp/foo \
  | sed '$d' && \
  [ "$(tail -1 /tmp/foo)" -eq 200 ]