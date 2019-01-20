#!/bin/bash

docker build -t foo .

docker run foo echo hello
