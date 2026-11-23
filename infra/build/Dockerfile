FROM ubuntu:24.04@sha256:c4a8d5503dfb2a3eb8ab5f807da5bc69a85730fb49b5cfca2330194ebcc41c7b

# Ubuntu packages come from this point-in-time snapshot of the archive instead of
# per-package version pins, which stop building as soon as Ubuntu supersedes a version.
# Renovate bumps it through the ubuntu-snapshot datasource in ScottG489/renovate-config.
ARG UBUNTU_SNAPSHOT=20261120T050039Z
RUN echo "APT::Snapshot \"${UBUNTU_SNAPSHOT}\";" > /etc/apt/apt.conf.d/50snapshot

# snapshot.ubuntu.com is HTTPS-only and the base image has no CA bundle yet, so the
# bootstrap skips TLS peer verification. apt still checks every index and package
# against the Ubuntu archive key, same as the image's default http:// sources.
RUN apt-get -o Acquire::https::Verify-Peer=false update && \
  apt-get -o Acquire::https::Verify-Peer=false install -y --no-install-recommends ca-certificates && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  git \
  openjdk-21-jdk \
  jq \
  ansible \
  curl \
  apt-transport-https \
  ca-certificates \
  gnupg \
  lsb-release \
  unzip

ARG VERSION_STRING=5:29.0.2-1~ubuntu.24.04~noble
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
  echo \
    "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
  apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  docker-ce=$VERSION_STRING \
  docker-ce-cli=$VERSION_STRING \
  containerd.io=1.7.28-1~ubuntu.24.04~noble

ARG TERRAFORM_VERSION=1.15.5
RUN curl https://releases.hashicorp.com/terraform/$TERRAFORM_VERSION/terraform_${TERRAFORM_VERSION}_linux_amd64.zip -o /tmp/terraform.zip && \
  unzip -d /usr/local/bin /tmp/terraform.zip && \
  rm /tmp/terraform.zip

ARG AWSCLI_VERSION=2.34.53
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64-$AWSCLI_VERSION.zip" -o "awscliv2.zip" && \
  unzip awscliv2.zip && \
  ./aws/install && \
  rm -rf aws awscliv2.zip

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
