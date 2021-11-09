provider "aws" {
  region = "us-west-2"
}

terraform {
  backend "s3" {
    bucket = "tfstate-alt-conjob"
    key = "app.tfstate"
    region = "us-west-2"
  }
}

module "helpers_spot_instance_ssh" {
  source = "ScottG489/helpers/aws//modules/spot_instance_ssh"
  version = "0.1.4"
  name = "${var.subdomain_name}.${var.second_level_domain_name}.${var.top_level_domain_name}"
  instance_type = var.instance_type
  spot_type = var.spot_type
  spot_price = var.spot_price
  volume_size = var.volume_size
  public_key = var.public_key
}

module "alt_conjob" {
  source = "./modules/conjob_core"
  domain_name = "${var.second_level_domain_name}.${var.top_level_domain_name}"
  subdomain_name = var.subdomain_name
  public_ip = module.helpers_spot_instance_ssh.public_ip
}

module "helpers_route53_domain_name_servers" {
  source  = "ScottG489/helpers/aws//modules/route53_domain_name_servers"
  version = "0.0.4"
  route53_zone_name = module.alt_conjob.r53_zone_name
  route53_zone_name_servers = module.alt_conjob.r53_zone_name_servers
}
