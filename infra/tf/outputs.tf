output "instance_public_ip" {
  value = aws_eip.eip.public_ip
}

output "certificate_p12" {
  value = acme_certificate.certificate.certificate_p12
  sensitive = true
}

output "keystore_password" {
  value = module.conjob.random_keystore_password
  sensitive = true
}