FROM ubuntu:24.04@sha256:9c7f200c07e3d4a03e7249d2c16bf66a51120fb876aa2242cf31451412543289

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  git=1:2.43.0-1ubuntu7.4 \
  openjdk-21-jdk=21.0.12.1+1-1~24.04.4 \
  jq=1.7.1-3ubuntu0.24.04.2 \
  ansible=9.2.0+dfsg-0ubuntu5 \
  curl=8.5.0-2ubuntu10.16 \
  apt-transport-https=2.8.3 \
  ca-certificates=20260816ubuntu0.24.04.1 \
  gnupg=2.4.4-2ubuntu17.6 \
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
