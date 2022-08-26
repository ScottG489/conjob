# ConJob
![CI](https://github.com/ScottG489/conjob/workflows/CI/badge.svg)

**ConJob** is a web service for running containers as jobs. It's intended to run specified images that will do some
work and then exit on their own. Any output from the job is returned to the caller.

## Usage
It's recommended to run **ConJob** using docker, but it can also be built then run from source.

### Docker
```shell
docker run -it \
  -p 8080:8080 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  scottg489/conjob
```

### Build and run from source
```shell script
git clone https://github.com/ScottG489/conjob.git \
  && cd conjob \
  && cp default-config.yml local-config.yml \
  && ./gradlew run --args="server local-config.yml"
```
`local-config.yml` can be edited to configure the server.

*Note: Java 11 or higher is required.*

### Make a request
```shell
curl 'localhost:8080/job/run?image=library/hello-world:latest'
```
Which should display the output of the `hello-world` image. Similar to if you run `docker run hello-world`.

You can also supply input:
```shell
curl -X POST --data 'foobar' 'localhost:8080/job/run?image=scottg489/echo-job:latest'
```
POST data is supplied as arguments to the application. Similar to if you run
`docker run scottg489/echo-job:latest foobar`.

## Development
### Common gradle tasks
- `unitTest` - Unit tests and [ArchUnit](https://www.archunit.org/) architecture tests
- `integrationTest` - Integration tests
- `run --args="server local-config.yml"` - Starts service using local config
- `acceptanceTest` - Acceptance tests
- `performanceTest` - Performance tests using [Gatling](https://gatling.io/)
- `piTest` - Mutation tests using [Pitest](https://pitest.org/)
- `jacocoApplicationReport` - Generate JaCoCo coverage report for application
- `jacocoAllTestReport` - Generate accumulative JaCoCo coverage report for all tests

### Test coverage
Unit and integration test coverage is captured via [JaCoCo](https://www.jacoco.org/jacoco/).

Acceptance test coverage is also capturing via JaCoCo. However, it's instrumented on the service itself.
In order for the coverage data file to be created, the service needs to be shut down.
After which the `jacocoAllTestReport` can be run.

Additionally, after coverage data files are generated, the `jacocoAllTestReport` task can be run to generate cumulative
coverage data for all tests.

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

The reason this is needed is that as part of it's deploy the server shuts itself down.
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

To remediate this issue you'll have to clean up the tfstate files in the appropriate backend-init directory.
