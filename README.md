# ConJob
![CI](https://github.com/ScottG489/conjob/workflows/CI/badge.svg)

## Project core philosophy

- Simplicity for the user
- Statelessness

## Usage
It's recommended to run **ConJob** using docker, but it can also be built then run from source.
## Docker
```shell script
docker run -it -v /var/run/docker.sock:/var/run/docker.sock \
  --name conjob scottg489/conjob
```
## Build and run from source
```shell script
git clone git@github.com:ScottG489/conjob.git && cd conjob
./gradlew install && ./build/install/ConJob/bin/ConJob server config.yml
```

## Development
### Building
You can build and run unit tests using the following:
```shell script
./gradlew build unitTest
```

### Installing and running within your project
You can install the server into your local project then run it using the following:
```shell script
./gradlew install && ./build/install/ConJob/bin/ConJob server config.yml
```

### Running acceptance tests
After running the server locally you can run acceptance tests against it with the following:
```shell script
./gradlew acceptanceTest
```

### Complete build testing
To fully test your changes run `./test.sh` at the root of the project. However, first make
sure to change the file locations of the secrets to your actual locations.

This script runs the complete build, publish, infrastructure provisioning, and deployment to
the test environment. It then runs the acceptance tests against the deployed service before
then tearing the environment down. This allows you to get a very high degree of certainty
that your changes will deploy successfully once pushed.

Note that you'll need to comment out the `git clone` in `infra/build/run.sh` otherwise it
will fail since you've mounted a directory where it will attempt to clone to.
You'll also probably want to comment out all the prod deploy steps unless you really
intend to deploy to prod from your local workstation.

### Creating alt/bootstrap server
The **ConJob** project uses **ConJob** itself as a CI server. This means that in order to build
the project you need an initial **ConJob** server running. 
In order to create this alternate/bootstrap server, simply run `./alt-env.sh`. It can also be run
to update the current alt server to the latest version.

The reason this is needed is because as part of it's deploy the server shuts itself down.
Although the build container will continue to run, the server will not return the build's
output, and we'll have no logs. At some point this will be replaced by a
[blue-green deployment](https://en.wikipedia.org/wiki/Blue-green_deployment).

### Development troubleshooting
```
+ terraform import aws_s3_bucket.backend_bucket tfstate-conjob
aws_s3_bucket.backend_bucket: Importing from ID "tfstate-conjob"...
aws_s3_bucket.backend_bucket: Import prepared!
  Prepared aws_s3_bucket for import

Error: Resource already managed by Terraform

Terraform is already managing a remote object for
aws_s3_bucket.backend_bucket. To import to this address you must first remove
the existing object from the state.
```

To remediate this issue you'll have to clean up the tfstate files in the appropriate backend-init directory
