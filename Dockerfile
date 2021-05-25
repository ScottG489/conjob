#FROM ubuntu:latest
FROM openjdk:13

COPY build/install/conjob /opt/conjob
COPY config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
