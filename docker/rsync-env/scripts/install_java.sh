#!/usr/bin/env bash

set -e -x

add-apt-repository -y ppa:webupd8team/java
apt-get -qq  update
apt-get install -y oracle-java8-installer
