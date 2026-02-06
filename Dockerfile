FROM eclipse-temurin:25@sha256:8b7be4d361d7aa906eb8767f08941dc861d7148d3d99e81b3e9b53e7e1b9c809

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
