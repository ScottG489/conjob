provider "aws" {
  region = "us-west-2"
}

resource "aws_s3_bucket" "docker_ci_prototype_backend_bucket" {
  bucket = "tfstate-docker-ci-prototype"
  force_destroy = true

  versioning {
    enabled = true
  }
}
