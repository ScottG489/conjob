provider "aws" {
  region = "us-west-2"
}

module "helpers_spot_instance_ssh" {
  source = "ScottG489/helpers/aws//modules/spot_instance_ssh"
  version = "1.5.0"
  ami = var.ami
  name = "${var.subdomain_name}.${random_id.second_level_domain_name.hex}.${var.top_level_domain_name}"
  instance_type = var.instance_type
  spot_type = var.spot_type
  instance_interruption_behavior = var.instance_interruption_behavior
  spot_price = var.spot_price
  volume_size = var.volume_size
  public_key = var.public_key
}

module "conjob" {
  source = "../modules/conjob_core"
  domain_name = "${random_id.second_level_domain_name.hex}.io"
  subdomain_name = var.subdomain_name
  public_ip = aws_eip.eip.public_ip
}

resource "random_id" "second_level_domain_name" {
  byte_length = 4
  prefix = "${var.second_level_domain_name}-"
}

resource "aws_eip" "eip" {
  vpc      = true
}

# The association is necessary because the aws_eip resource requires the instance be available, which it may not be since it's a spot instance.
resource "aws_eip_association" "eip_assoc" {
  instance_id = module.helpers_spot_instance_ssh.spot_instance_id
  allocation_id = aws_eip.eip.id
}
