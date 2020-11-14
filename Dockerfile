#FROM ubuntu:latest
FROM openjdk:11

COPY build/install/ConJob /opt/ConJob
COPY config.yml /opt/app/config.yml
CMD ["/opt/ConJob/bin/ConJob", "server", "/opt/app/config.yml"]
