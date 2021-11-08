provider "aws" {
  region = "us-west-2"
}

module "helpers_spot_instance_ssh" {
  source = "ScottG489/helpers/aws//modules/spot_instance_ssh"
  version = "0.0.4"
  name = "${var.subdomain_name}.${random_id.second_level_domain_name.hex}.${var.top_level_domain_name}"
  instance_type = var.instance_type
  spot_type = var.spot_type
  spot_price = var.spot_price
  volume_size = var.volume_size
  public_key = var.public_key
}

module "conjob" {
  source = "../modules/conjob_core"
  domain_name = "${random_id.second_level_domain_name.hex}.io"
  subdomain_name = var.subdomain_name
  public_ip = module.helpers_spot_instance_ssh.public_ip
}

resource "random_id" "second_level_domain_name" {
  byte_length = 4
  prefix = "${var.second_level_domain_name}-"
}
