FROM eclipse-temurin:25@sha256:e23592541431eaeef5c13c84c21db71f97cdca0e70181ea6222ec9bccac24f6c

COPY build/install/conjob /opt/conjob
COPY default-config.yml /opt/app/config.yml
CMD ["/opt/conjob/bin/conjob", "server", "/opt/app/config.yml"]
