provider "aws" {
  region = "us-west-2"
}
provider "aws" {
  region = "us-east-1"
  alias = "us_east_1"
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
  version = "0.1.9"
  name = "alt-${var.domain_name}"
  instance_type = var.instance_type
  spot_type = var.spot_type
  spot_price = var.spot_price
  volume_size = var.volume_size
  public_key = var.public_key
}

module "alt_conjob" {
  source = "./modules/conjob_core"
  domain_name = var.domain_name
  subdomain_name = var.subdomain_name
  cf_dist_zone_id = aws_cloudfront_distribution.cloudfront_dist.hosted_zone_id
  cf_dist_domain_name = aws_cloudfront_distribution.cloudfront_dist.domain_name
}

module "helpers_route53_domain_name_servers" {
  source  = "ScottG489/helpers/aws//modules/route53_domain_name_servers"
  version = "0.1.9"
  route53_zone_name = module.alt_conjob.r53_zone_name
  route53_zone_name_servers = module.alt_conjob.r53_zone_name_servers
}

# aws_cloudfront_distribution.cloudfront_dist:
resource "aws_cloudfront_distribution" "cloudfront_dist" {
  provider = aws.us_east_1
  enabled                        = true
  is_ipv6_enabled                = true
  
  aliases  = [
    "${var.subdomain_name}.${var.domain_name}",
  ]

  default_cache_behavior {
    allowed_methods          = [
      "DELETE",
      "GET",
      "HEAD",
      "OPTIONS",
      "PATCH",
      "POST",
      "PUT",
    ]
    cached_methods           = [
      "GET",
      "HEAD",
    ]
    cache_policy_id          = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
    compress                 = false
    origin_request_policy_id = "216adef6-5c7f-47e4-b989-5492eafa07d3"
    target_origin_id         = module.helpers_spot_instance_ssh.public_dns
    viewer_protocol_policy   = "redirect-to-https"
  }

  origin {
    domain_name = module.helpers_spot_instance_ssh.public_dns
    origin_id   = module.helpers_spot_instance_ssh.public_dns

    custom_origin_config {
      https_port = 443
      http_port = 80
      origin_protocol_policy   = "http-only"
      origin_ssl_protocols     = [
        "TLSv1.2",
      ]
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn            = aws_acm_certificate.acm_cert.arn
    cloudfront_default_certificate = false
    minimum_protocol_version       = "TLSv1.2_2021"
    ssl_support_method             = "sni-only"
  }
}

resource "aws_acm_certificate" "acm_cert" {
  // Certs used with CF need to be in us-east-1
  provider = aws.us_east_1
  domain_name = "${var.subdomain_name}.${var.domain_name}"
  validation_method = "DNS"
}

resource "aws_acm_certificate_validation" "cert_validation" {
  provider = aws.us_east_1
  certificate_arn         = aws_acm_certificate.acm_cert.arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation_r53_record_cname : record.fqdn]
}

resource "aws_route53_record" "cert_validation_r53_record_cname" {
  zone_id = module.alt_conjob.r53_zone_id
  name            = each.value.name
  records         = [each.value.record]
  type            = each.value.type
  ttl             = 60

  for_each = {
  for dvo in aws_acm_certificate.acm_cert.domain_validation_options : dvo.domain_name => {
    name   = dvo.resource_record_name
    record = dvo.resource_record_value
    type   = dvo.resource_record_type
  }
  }
}
