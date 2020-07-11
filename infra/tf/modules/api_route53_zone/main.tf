resource "aws_route53_zone" "api_r53_zone" {
  name = var.name
}

resource "aws_route53_record" "api_r53_record_A_top" {
  zone_id = aws_route53_zone.api_r53_zone.id
  name    = ""
  records = [
    var.public_ip
  ]
  ttl     = 300
  type    = "A"
}

resource "aws_route53_record" "api_r53_record_A_api" {
  zone_id = aws_route53_zone.api_r53_zone.id
  name    = "api"
  records = [
    var.public_ip
  ]
  ttl     = 300
  type    = "A"
}
