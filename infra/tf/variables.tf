variable "second_level_domain_name" {}
variable "top_level_domain_name" {}
variable "subdomain_name" { }
variable "public_key" {}
variable "keystore_password" {
  sensitive = true
}
