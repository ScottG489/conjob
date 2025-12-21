#FROM ubuntu:latest
FROM eclipse-temurin:11@sha256:f16caedceea97fd2591a122c6515481ec2961b908c83e101313aacd96ae3439d

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
