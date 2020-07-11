resource "aws_spot_instance_request" "spot_instance_request" {
  ami           = "ami-09dd2e08d601bff67"
  instance_type = "t2.medium"
  vpc_security_group_ids = [aws_security_group.security_group.id]
  key_name = aws_key_pair.key_pair.key_name

  spot_type = "one-time"
  spot_price    = "0.015"
  wait_for_fulfillment = true

  root_block_device {
    volume_type           = "gp2"
    volume_size           = 8
  }

  tags = {
    Name = var.name
  }

  provisioner "local-exec" {
    command = "${path.module}/scripts/create_instance_tag.sh ${aws_spot_instance_request.spot_instance_request.spot_instance_id} ${var.name}"
  }
}

resource "aws_security_group" "security_group" {
  name = var.name
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_key_pair" "key_pair" {
  key_name   = var.name
  public_key = var.public_key
}
