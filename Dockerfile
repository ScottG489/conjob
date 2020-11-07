#FROM ubuntu:latest
FROM openjdk:11

RUN mkdir /opt/docker-ci-prototype/
COPY build/install/docker-ci-prototype /opt/docker-ci-prototype
COPY config.yml /opt/app/config.yml
CMD ["/opt/docker-ci-prototype/bin/docker-ci-prototype", "server", "/opt/app/config.yml"]
