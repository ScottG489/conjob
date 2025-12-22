FROM ubuntu:24.04

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  git \
  openjdk-11-jdk \
  jq \
  ansible \
  curl \
  apt-transport-https \
  ca-certificates \
  gnupg \
  lsb-release \
  unzip

RUN curl https://releases.hashicorp.com/terraform/0.14.0/terraform_0.14.0_linux_amd64.zip -o /tmp/terraform.zip && \
  unzip -d /usr/local/bin /tmp/terraform.zip && \
  rm /tmp/terraform.zip

COPY known_hosts /root/.ssh/known_hosts

RUN mkdir /root/.aws
COPY config /root/.aws/config

RUN mkdir -p /opt/build
COPY run.sh /opt/build/run.sh
COPY build_functions.sh /opt/build/build_functions.sh

WORKDIR /opt/build
ENTRYPOINT ["./run.sh"]
