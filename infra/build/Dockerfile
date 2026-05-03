FROM ubuntu:26.04@sha256:5e275723f82c67e387ba9e3c24baa0abdcb268917f276a0561c97bef9450d0b4

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  git=1:2.53.0-1ubuntu1 \
  openjdk-21-jdk=21.0.11~8ea-1 \
  jq=1.8.1-4ubuntu2 \
  ansible=13.1.0+dfsg-1ubuntu1 \
  curl=8.18.0-1ubuntu2 \
  apt-transport-https=3.2.0 \
  ca-certificates=20260223 \
  gnupg=2.4.8-4ubuntu3 \
  lsb-release=12.1-2build1 \
  unzip=6.0-29ubuntu1

ARG VERSION_STRING=5:29.4.1-1~ubuntu.26.04~resolute
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
  echo \
    "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
  apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  docker-ce=$VERSION_STRING \
  docker-ce-cli=$VERSION_STRING \
  containerd.io=2.2.3-1~ubuntu.26.04~resolute

ARG TERRAFORM_VERSION=1.14.9
RUN curl https://releases.hashicorp.com/terraform/$TERRAFORM_VERSION/terraform_${TERRAFORM_VERSION}_linux_amd64.zip -o /tmp/terraform.zip && \
  unzip -d /usr/local/bin /tmp/terraform.zip && \
  rm /tmp/terraform.zip

ARG AWSCLI_VERSION=2.34.41
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
