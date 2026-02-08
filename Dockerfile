FROM eclipse-temurin:25@sha256:5b691907413c1c67b1f2402a39c64736d02404e703aa6f688164be23f83f06c4

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
