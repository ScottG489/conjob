#FROM ubuntu:latest
FROM openjdk:11

RUN mkdir /opt/docker-ci-prototype/
COPY build/install/ConJob /opt/ConJob
COPY config.yml /opt/app/config.yml
CMD ["/opt/ConJob/bin/ConJob", "server", "/opt/app/config.yml"]
