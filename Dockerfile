FROM eclipse-temurin:25@sha256:bee2e23ab444ed60daf8123e36478bc4a286ba7835bec6f9daf9eba1d50a86a2

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
