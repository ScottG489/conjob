output "instance_public_ip" {
  value = module.helpers_instance_ssh.public_ip
}

output "certificate_p12" {
  value = acme_certificate.certificate.certificate_p12
  sensitive = true
}
