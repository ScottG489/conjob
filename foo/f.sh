#!/usr/bin/env bash

container_name="helloworldcontainer"

docker rm $container_name || echo "Container $container_name does not exist"
docker run --name $container_name busybox "echo" "Hello World"
wait

for i in `seq 0 1000`; do

    output=$(docker logs "helloworldcontainer");
    if [ "$output" != "Hello World" ]; then
        echo "Container $i did not return the expected output. It returned $output instead"
    fi

done
