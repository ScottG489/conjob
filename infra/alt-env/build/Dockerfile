FROM ubuntu:24.04@sha256:c4a8d5503dfb2a3eb8ab5f807da5bc69a85730fb49b5cfca2330194ebcc41c7b

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  git=1:2.43.0-1ubuntu7.3 \
  openjdk-21-jdk=21.0.10+7-1~24.04 \
  jq=1.7.1-3ubuntu0.24.04.2 \
  ansible=9.2.0+dfsg-0ubuntu5 \
  curl=8.5.0-2ubuntu10.8 \
  apt-transport-https=2.8.3 \
  ca-certificates=20240203 \
  gnupg=2.4.4-2ubuntu17.4 \
  lsb-release=12.0-2 \
  unzip=6.0-28ubuntu4.1

COPY known_hosts /root/.ssh/known_hosts

RUN mkdir /root/.aws
COPY config /root/.aws/config

RUN mkdir -p /opt/build
COPY run.sh /opt/build/run.sh
COPY build_functions.sh /opt/build/build_functions.sh

WORKDIR /opt/build
ENTRYPOINT ["./run.sh"]
