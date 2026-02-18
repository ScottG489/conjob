FROM eclipse-temurin:25@sha256:acab08ae09273ee938c1da6111ed60ff51ab0ab18325e4b1b81178039059f86e

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
