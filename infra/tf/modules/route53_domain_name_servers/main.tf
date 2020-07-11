resource "null_resource" "route53_domain_name_servers" {
  triggers = {
    name_servers = join(",", var.route53_zone_name_servers)
  }

  provisioner "local-exec" {
    command = "${path.module}/scripts/update_r53_zone_nameservers.sh ${var.route53_zone_name} ${join(" ", var.route53_zone_name_servers)}"
  }
}
