output "name" {
  value = aws_route53_zone.api_r53_zone.name
}

output "nameservers" {
  value = aws_route53_zone.api_r53_zone.name_servers
}
