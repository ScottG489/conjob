provider "aws" {
  region = "us-west-2"
}

resource "aws_spot_instance_request" "spot_instance_request" {
  ami           = "ami-09dd2e08d601bff67"
  instance_type = "t2.medium"
  vpc_security_group_ids = [aws_security_group.sg.id]
  key_name = aws_key_pair.key_pair.key_name

  spot_type = "one-time"
  spot_price    = "0.03"
  wait_for_fulfillment = true

  root_block_device {
    volume_type           = "gp2"
    volume_size           = 8
  }

  tags = {
    Name = "${var.spot_instance_tag_name}_${random_uuid.rand.result}"
  }

  provisioner "local-exec" {
    command = "aws ec2 create-tags --resources ${aws_spot_instance_request.spot_instance_request.spot_instance_id} --tag --tags Key=Name,Value=${var.instance_tag_name}_${random_uuid.rand.result}"
  }
}

resource "aws_security_group" "sg" {
  name = "${var.sg_name}_${random_uuid.rand.result}"
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
  key_name   = "${var.key_pair_name}_${random_uuid.rand.result}"
  public_key = var.public_key
}

resource "random_uuid" "rand" { }