#!/usr/bin/env bash

docker build -t rsynk/integration-tests .

docker run rsynk/integration-tests
