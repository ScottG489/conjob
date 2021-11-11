terraform {
  backend "s3" {
    bucket = "tfstate-conjob"
    key = "app.tfstate"
    region = "us-west-2"
  }
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
    acme = {
      source  = "vancluever/acme"
      version = "~> 2.0"
    }
  }
  required_version = ">= 0.13"
}
