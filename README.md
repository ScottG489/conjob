# docker-ci-prototype
![CI](https://github.com/ScottG489/docker-ci-prototype/workflows/CI/badge.svg)

## Core philosophy

- Simplicity for the user
- Statelessness

## Development
Here is an example of developing the build in conjunction with the application locally.
Make sure you change the file locations of the desired secrets to your actual location.

```bash
ID_RSA_CONTENTS_BASE64=$(base64 ~/.ssh/id_rsa | tr -d '\n') ;
AWS_CREDENTIALS_CONTENTS_BASE64=$(base64 ~/.aws/credentials | tr -d '\n') ;
MAINKEYPAIR_CONTENTS_BASE64=$(base64 ~/.ssh/mainkeypair.pem | tr -d '\n') ;
DOCKER_CONFIG_CONTENTS_BASE64=$(base64 ~/.docker/config.json | tr -d '\n') ;
docker build infra/build -t simple-ci-test && \
docker run -it --volume "$PWD:/opt/build/docker-ci-prototype" -v /var/run/docker.sock:/var/run/docker.sock simple-ci-test '{"ID_RSA": "'"$ID_RSA_CONTENTS_BASE64"'", "AWS_CREDENTIALS": "'"$AWS_CREDENTIALS_CONTENTS_BASE64"'", "MAIN_KEY_PAIR": "'"$MAINKEYPAIR_CONTENTS_BASE64"'", "DOCKER_CONFIG": "'"$DOCKER_CONFIG_CONTENTS_BASE64"'"}'
```

1. Initialize the secrets as envars (these will be passed in as the arguments to the container)
2. Build the image locally
3. Run the image with the path to your local repository mounted where the code would normally be cloned to

Note that you'll need to comment out the `git clone` in the build otherwise it will fail since you've mounted a directory there