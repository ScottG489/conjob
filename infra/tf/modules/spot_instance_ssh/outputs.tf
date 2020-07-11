output "public_ip" {
  value = aws_spot_instance_request.spot_instance_request.public_ip
}
