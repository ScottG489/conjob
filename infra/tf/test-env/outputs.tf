output "instance_public_ip" {
  value = module.helpers_spot_instance_ssh.public_ip
}

output "df_dist_domain_name" {
  value = aws_cloudfront_distribution.cloudfront_dist.domain_name
}