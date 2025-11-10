#FROM ubuntu:latest
FROM eclipse-temurin:11

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
