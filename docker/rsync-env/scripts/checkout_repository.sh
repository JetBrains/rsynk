#!/usr/bin/env bash

set -e -x

apt-get -qq update
apt-get install -y git
git clone ${JB_REPO_URL} ${JB_CHECKOUT_DIR}
