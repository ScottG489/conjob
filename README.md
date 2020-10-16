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

### Creating alt/bootstrap server
In order for the service to deploy itself there needs to be a service already running it can build on.
Otherwise, it will shut itself down mid-deploy. In order to create this alternate server, run the same command
above but change the docker build directory to `infra/alt-env/build`.

### Development troubleshooting
```
+ terraform import aws_s3_bucket.backend_bucket tfstate--docker-ci-prototype
aws_s3_bucket.backend_bucket: Importing from ID "tfstate--docker-ci-prototype"...
aws_s3_bucket.backend_bucket: Import prepared!
  Prepared aws_s3_bucket for import

Error: Resource already managed by Terraform

Terraform is already managing a remote object for
aws_s3_bucket.backend_bucket. To import to this address you must first remove
the existing object from the state.
```

To remediate this issue you'll have to clean up the tfstate files in the appropriate backend-init directory
