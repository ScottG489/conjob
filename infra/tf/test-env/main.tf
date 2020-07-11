provider "aws" {
  region = "us-west-2"
}

module "spot_instance_ssh" {
  source = "../modules/spot_instance_ssh"
  name = random_id.name_prefix.hex
  public_key = var.public_key
}

module "api_route53_zone" {
  source = "../modules/api_route53_zone"
  name = "${random_id.name_prefix.hex}.com"
  public_ip = module.spot_instance_ssh.public_ip
}

resource "random_id" "name_prefix" {
  byte_length = 4
  prefix = "${var.name}-"
}