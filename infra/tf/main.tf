provider "aws" {
  region = "us-west-2"
}

terraform {
  backend "s3" {
    bucket = "tfstate-docker-ci-prototype"
    key    = "docker-ci-prototype.tfstate"
    region = "us-west-2"
  }
}

resource "aws_instance" "docker_ci_prototype_instance" {
  ami           = "ami-09dd2e08d601bff67"
  instance_type = "t2.small"
  vpc_security_group_ids = [aws_security_group.docker_ci_prototype_sg.id]
  key_name = aws_key_pair.docker_ci_prototype_key.key_name

  root_block_device {
    volume_type           = "gp2"
    volume_size           = 40
  }

  tags = {
    Name = "docker-ci-prototype"
  }
}

resource "aws_security_group" "docker_ci_prototype_sg" {
  name = "docker-ci-prototype-sg"
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

resource "aws_key_pair" "docker_ci_prototype_key" {
  key_name   = "docker-ci-prototype-key"
  public_key = var.public_key
}

resource "aws_route53_zone" "docker_ci_prototype_r53_zone" {
    name         = "simple-ci.com"
}

resource "aws_route53_record" "docker_ci_prototype_r53_record_A" {
    zone_id = aws_route53_zone.docker_ci_prototype_r53_zone.id
    name    = ""
    records = [
        "${aws_instance.docker_ci_prototype_instance.public_ip}",
    ]
    ttl     = 300
    type    = "A"
}
