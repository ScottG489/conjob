FROM eclipse-temurin:25@sha256:10331564d9ae41b6a534ddea472f37270a3c286e89857261631a0d772a4d8617

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
