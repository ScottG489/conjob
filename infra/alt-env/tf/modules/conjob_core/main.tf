provider "aws" {
  region = "us-west-2"
}

resource "aws_route53_zone" "r53_zone" {
  name = var.domain_name
}

resource "aws_route53_record" "r53_record_A_api" {
  zone_id = aws_route53_zone.r53_zone.id
  name = var.subdomain_name
  records = [
    var.public_ip
  ]
  ttl = 300
  type = "A"
}

resource "random_id" "random_keystore_password" {
  byte_length = 8
}
