FROM ubuntu:20.04

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  git \
  awscli \
  docker.io \
  openjdk-11-jdk \
  jq \
  ansible \
  curl \
  unzip

RUN curl https://releases.hashicorp.com/terraform/0.12.26/terraform_0.12.26_linux_amd64.zip -o /tmp/terraform.zip && \
  unzip -d /usr/local/bin /tmp/terraform.zip && \
  rm /tmp/terraform.zip

RUN mkdir /root/.ssh
COPY known_hosts /root/.ssh/known_hosts

RUN mkdir /root/.aws
COPY config /root/.aws/config

RUN mkdir /root/.docker

RUN mkdir -p /opt/build
COPY run.sh /opt/build/run.sh
COPY run-test.sh /opt/build/run-test.sh
COPY build_functions.sh /opt/build/build_functions.sh

WORKDIR /opt/build
ENTRYPOINT ["./run.sh"]
