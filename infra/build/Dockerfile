FROM ubuntu:20.04

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  git \
  awscli \
  openjdk-11-jdk \
  iptables \
  jq \
  ansible \
  curl \
  unzip

RUN curl https://download.docker.com/linux/static/stable/x86_64/docker-20.10.2.tgz -o /tmp/docker.tgz && \
  tar xzvf /tmp/docker.tgz && \
  cp docker/* /usr/bin/ && \
  rm -rf docker /tmp/docker.tgz

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
