provider "aws" {
  region = "us-west-2"
}
provider "aws" {
  region = "us-east-1"
  alias = "us_east_1"
}

module "helpers_spot_instance_ssh" {
  source = "ScottG489/helpers/aws//modules/spot_instance_ssh"
  version = "0.1.9"
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
  subdomain_name = var.subdomain_name
  public_ip = module.helpers_spot_instance_ssh.public_ip
}

resource "random_id" "name_prefix" {
  byte_length = 4
  prefix = "${var.domain_name}-"
}

resource "aws_cloudfront_distribution" "cloudfront_dist" {
  provider = aws.us_east_1
  enabled                        = true
  is_ipv6_enabled                = true

  default_cache_behavior {
    allowed_methods = [
      "DELETE",
      "GET",
      "HEAD",
      "OPTIONS",
      "PATCH",
      "POST",
      "PUT",
    ]
    cached_methods = [
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
    cloudfront_default_certificate = true
  }
}
