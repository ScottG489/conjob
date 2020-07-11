provider "aws" {
  region = "us-west-2"
}

terraform {
  backend "s3" {
    bucket = "tfstate-docker-ci-prototype"
    key    = "docker-ci-prototype.tfstate"
    region = "us-west-2"
  }
}

module "instance" {
  source = "./modules/instance_ssh"
  name = var.name
  public_key = var.public_key
}

module "api_route53_zone" {
  source = "./modules/api_route53_zone"
  name = var.name
  public_ip = module.instance.public_ip
}

module "route53_domain_name_servers" {
  source = "./modules/route53_domain_name_servers"
  route53_zone_name = module.api_route53_zone.name
  route53_zone_name_servers = module.api_route53_zone.nameservers
}
