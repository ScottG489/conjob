#!/bin/bash
set -x

sleep 30
# Wait for service to come up before considering the playbook finished
# IPv4 flag is required due to docker weirdness: https://github.com/appropriate/docker-curl/issues/5
curl -sS --ipv4 --retry-connrefused --retry 5 localhost:80