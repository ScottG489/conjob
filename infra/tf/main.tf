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
  version = "0.0.4"
  name = var.domain_name
  public_key = var.public_key
}

module "simple_ci" {
  source = "./modules/simple_ci_core"
  domain_name = var.domain_name
  public_ip = module.helpers_instance_ssh.public_ip
}

module "helpers_route53_domain_name_servers" {
  source  = "ScottG489/helpers/aws//modules/route53_domain_name_servers"
  version = "0.0.4"
  route53_zone_name = module.simple_ci.r53_zone_name
  route53_zone_name_servers = module.simple_ci.r53_zone_name_servers
}
