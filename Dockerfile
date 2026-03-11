FROM eclipse-temurin:25@sha256:dd23c917b42d5ba34b726c3b339ba0f71fac76a8bdebb936b511bb98832dc287

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
