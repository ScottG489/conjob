curl -v -s --data-binary '{"ID_RSA": "'"$1"'", "DOCKER_CONFIG": "'"$2"'", "AWS_CREDENTIALS": "'"$3"'"}' 'https://diff-data.com/build?image=scottg489/docker-ci-prototype-build:latest'
