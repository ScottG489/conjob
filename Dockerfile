FROM ubuntu:18.10

RUN apt-get update
RUN apt-get install -y docker.io

RUN echo 'FROM ubuntu:18.10' > /opt/Dockerfile

COPY build.sh /opt/build.sh

WORKDIR /opt
CMD ["./build.sh"]
