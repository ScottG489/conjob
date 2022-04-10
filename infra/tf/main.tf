provider "aws" {
  region = "us-west-2"
}

module "helpers_spot_instance_ssh" {
  source = "ScottG489/helpers/aws//modules/spot_instance_ssh"
  version = "1.2.0"
  name = "${var.subdomain_name}.${var.second_level_domain_name}.${var.top_level_domain_name}"
  instance_type = var.instance_type
  spot_type = var.spot_type
  instance_interruption_behavior = var.instance_interruption_behavior
  spot_price = var.spot_price
  volume_size = var.volume_size
  public_key = var.public_key
}

module "conjob" {
  source = "./modules/conjob_core"
  domain_name = "${var.second_level_domain_name}.${var.top_level_domain_name}"
  subdomain_name = var.subdomain_name
  public_ip = aws_eip.eip.public_ip
}

module "helpers_route53_domain_name_servers" {
  source  = "ScottG489/helpers/aws//modules/route53_domain_name_servers"
  version = "0.0.4"
  route53_zone_name = module.conjob.r53_zone_name
  route53_zone_name_servers = module.conjob.r53_zone_name_servers
}

resource "aws_eip" "eip" {
  vpc      = true
}

# The association is necessary because the aws_eip resource requires the instance be in a running status, which it may not be even if terraform considers the resource created.
resource "aws_eip_association" "eip_assoc" {
  instance_id = module.helpers_spot_instance_ssh.spot_instance_id
  allocation_id = aws_eip.eip.id
}

provider "acme" {
    server_url = "https://acme-v02.api.letsencrypt.org/directory"
}

resource "tls_private_key" "private_key" {
  algorithm = "RSA"
}

resource "acme_registration" "reg" {
  depends_on = [
    module.conjob,
  ]
  account_key_pem = tls_private_key.private_key.private_key_pem
  email_address   = "nobody@gmail.com"
}

resource "acme_certificate" "certificate" {
  account_key_pem           = acme_registration.reg.account_key_pem
  certificate_p12_password  = module.conjob.random_keystore_password
  common_name               = "${var.subdomain_name}.${var.second_level_domain_name}.${var.top_level_domain_name}"

  dns_challenge {
    provider = "route53"
  }
}
