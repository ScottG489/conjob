provider "aws" {
  region = "us-west-2"
}

resource "aws_s3_bucket" "backend_bucket" {
  bucket = "tfstate-alt-docker-ci-prototype"
  force_destroy = true

  versioning {
    enabled = true
  }
}
