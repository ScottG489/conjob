provider "aws" {
  region = "us-west-2"
}

resource "aws_route53_zone" "r53_zone" {
  name = var.domain_name
}

resource "aws_route53_record" "r53_record_A_api" {
  zone_id = aws_route53_zone.r53_zone.id
  name = var.subdomain_name
  type = "A"

  alias {
    zone_id                = var.cf_dist_zone_id
    name                   = var.cf_dist_domain_name
    evaluate_target_health = false
  }
}
