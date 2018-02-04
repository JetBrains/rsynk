#!/usr/bin/env bash

docker build -t rsync-env .

docker run -i rsync-env
