# ConJob
![CI](https://github.com/ScottG489/conjob/workflows/CI/badge.svg)

## Core philosophy

- Simplicity for the user
- Statelessness

## Development
To fully test your changes run `./test.sh` at the root of the project. However, first make sure to change the file locations of the secrets to your actual locations.

Note that you'll need to comment out the `git clone` in `run.sh` otherwise it will fail since you've mounted a directory where it will attempt to clone to. You'll also probably want to comment out all of the prod deploy steps unless you really intend to deploy to prod from your local workstation.

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
