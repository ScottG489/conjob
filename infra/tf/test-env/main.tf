provider "aws" {
  region = "us-west-2"
}

module "helpers_spot_instance_ssh" {
  source = "ScottG489/helpers/aws//modules/spot_instance_ssh"
  version = "0.0.4"
  name = "${random_id.name_prefix.hex}.io"
  instance_type = var.instance_type
  spot_type = var.spot_type
  spot_price = var.spot_price
  volume_size = var.volume_size
  public_key = var.public_key
}

module "conjob" {
  source = "../modules/conjob_core"
  domain_name = "${random_id.name_prefix.hex}.io"
  public_ip = module.helpers_spot_instance_ssh.public_ip
}

resource "random_id" "name_prefix" {
  byte_length = 4
  prefix = "${var.domain_name}-"
}
