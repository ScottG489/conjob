FROM ubuntu:22.04@sha256:104ae83764a5119017b8e8d6218fa0832b09df65aae7d5a6de29a85d813da2fb

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  git=1:2.34.1-1ubuntu1.15 \
  openjdk-21-jdk=21.0.9+10-1~22.04 \
  jq=1.6-2.1ubuntu3.1 \
  ansible=2.10.7+merged+base+2.10.8+dfsg-1 \
  curl=7.81.0-1ubuntu1.21 \
  apt-transport-https=2.7.14build2 \
  ca-certificates=20240203~22.04.1 \
  gnupg=2.2.27-3ubuntu2.4 \
  lsb-release=11.1.0ubuntu4 \
  unzip=6.0-26ubuntu3.1

ARG VERSION_STRING=5:24.0.9-1~ubuntu.22.04~jammy
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
  echo \
    "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
  apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  docker-ce=$VERSION_STRING \
  docker-ce-cli=$VERSION_STRING \
  containerd.io=1.7.28-1~ubuntu.22.04~jammy

ARG TERRAFORM_VERSION=1.14.3
RUN curl https://releases.hashicorp.com/terraform/$TERRAFORM_VERSION/terraform_${TERRAFORM_VERSION}_linux_amd64.zip -o /tmp/terraform.zip && \
  unzip -d /usr/local/bin /tmp/terraform.zip && \
  rm /tmp/terraform.zip

ARG AWSCLI_VERSION=2.32.24
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64-$AWSCLI_VERSION.zip" -o "awscliv2.zip" && \
  unzip awscliv2.zip && \
  ./aws/install && \
  rm -rf aws awscliv2.zip \

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
