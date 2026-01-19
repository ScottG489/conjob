FROM eclipse-temurin:25@sha256:42fc3fe6804ec612f5ef8a613f8c06d8dd578de6207336077387d4cb32edaa9b

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
