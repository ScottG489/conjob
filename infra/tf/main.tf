provider "aws" {
  region = "us-west-2"
}

module "helpers_instance_ssh" {
  source = "ScottG489/helpers/aws//modules/instance_ssh"
  version = "0.0.4"
  name = "${var.subdomain_name}.${var.second_level_domain_name}.${var.top_level_domain_name}"
  public_key = var.public_key
}

module "conjob" {
  source = "./modules/conjob_core"
  domain_name = "${var.second_level_domain_name}.${var.top_level_domain_name}"
  subdomain_name = var.subdomain_name
  public_ip = module.helpers_instance_ssh.public_ip
}

module "helpers_route53_domain_name_servers" {
  source  = "ScottG489/helpers/aws//modules/route53_domain_name_servers"
  version = "0.0.4"
  route53_zone_name = module.conjob.r53_zone_name
  route53_zone_name_servers = module.conjob.r53_zone_name_servers
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
  account_key_pem = "${tls_private_key.private_key.private_key_pem}"
  email_address   = "nobody@gmail.com"
}

resource "acme_certificate" "certificate" {
  account_key_pem           = "${acme_registration.reg.account_key_pem}"
  certificate_p12_password  = module.conjob.random_keystore_password
  common_name               = "${var.subdomain_name}.${var.second_level_domain_name}.${var.top_level_domain_name}"

  dns_challenge {
    provider = "route53"
  }
}