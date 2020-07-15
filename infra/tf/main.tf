provider "aws" {
  region = "us-west-2"
}

terraform {
  backend "s3" {
    bucket = "tfstate-docker-ci-prototype"
    key = "docker-ci-prototype.tfstate"
    region = "us-west-2"
  }
}

module "helpers_instance_ssh" {
  source = "ScottG489/helpers/aws//modules/instance_ssh"
  version = "0.0.1"
  name = var.name
  public_key = var.public_key
}

module "helpers_api_route53_zone" {
  source = "ScottG489/helpers/aws//modules/api_route53_zone"
  version = "0.0.1"
  name = var.name
  public_ip = module.helpers_instance_ssh.public_ip
}

module "helpers_route53_domain_name_servers" {
  source  = "ScottG489/helpers/aws//modules/route53_domain_name_servers"
  version = "0.0.1"
  route53_zone_name = module.helpers_api_route53_zone.name
  route53_zone_name_servers = module.helpers_api_route53_zone.nameservers
}
