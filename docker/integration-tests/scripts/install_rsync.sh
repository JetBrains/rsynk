#!/usr/bin/env bash

set -e -x

apt-get install -y wget gcc make

mkdir rsync && cd rsync
wget https://download.samba.org/pub/rsync/src/rsync-3.1.3.tar.gz
tar -xf rsync-3.1.3.tar.gz

cd rsync-3.1.3
./configure
make
ln -s $(pwd)/rsync ${JB_DEPENDENCIES_DIR}/rsync

rsync --version


