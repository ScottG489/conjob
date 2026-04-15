FROM eclipse-temurin:25@sha256:656533f346d29cee151e2a7b697c84c878ea40cc9d04ba8bab59784d8c1e6299

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
