FROM ubuntu:18.10

RUN apt-get update

# TODO: Remove python-pip
RUN apt-get install -y git python-pip
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y awscli
# TODO: Remove this
RUN pip install awsebcli

RUN apt-get install -y docker.io
RUN apt-get install -y openjdk-11-jre

RUN mkdir /root/.ssh
COPY known_hosts /root/.ssh/known_hosts

RUN mkdir /root/.aws
COPY config /root/.aws/config

RUN mkdir /root/.docker

RUN mkdir -p /opt/build
COPY build.sh /opt/build/build.sh

WORKDIR /opt/build
ENTRYPOINT ["./build.sh"]
