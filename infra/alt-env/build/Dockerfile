FROM ubuntu:24.04@sha256:cd1dba651b3080c3686ecf4e3c4220f026b521fb76978881737d24f200828b2b

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
  git=1:2.43.0-1ubuntu7.3 \
  openjdk-11-jdk=11.0.29+7-1ubuntu1~24.04 \
  jq=1.7.1-3ubuntu0.24.04.1 \
  ansible=9.2.0+dfsg-0ubuntu5 \
  curl=8.5.0-2ubuntu10.6 \
  apt-transport-https=2.7.14build2 \
  ca-certificates=20240203 \
  gnupg=2.4.4-2ubuntu17.3 \
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
