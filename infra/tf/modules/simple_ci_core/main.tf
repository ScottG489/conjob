provider "aws" {
  region = "us-west-2"
}

resource "aws_route53_zone" "r53_zone" {
  name = var.domain_name
}

resource "aws_route53_record" "r53_record_A_api" {
  zone_id = aws_route53_zone.r53_zone.id
  name = "api"
  records = [
    var.public_ip
  ]
  ttl = 300
  type = "A"
}
