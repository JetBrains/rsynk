FROM ubuntu:16.04

# Image consist of:
#   openjdk 8
#   rsync-3.1.3
#   latest git from ubuntu repositories
#
# The main purpose is to create an isolated environment with strongly
# defined rsync client version and run rsynk integration tests inside
# the same environment across different OS.

# Currently the repository gets checked-out twice
# Perhaps, docker image should dwell in a separate repo
# or being distributed via docker hub

ENV JB_REPO_URL "https://github.com/jetbrains/rsynk.git"
ENV JB_CHECKOUT_DIR "/rsynk"
ENV JB_DEPENDENCIES_DIR "/dependencies"
ENV PATH="${JB_DEPENDENCIES_DIR}:${PATH}"
ENV JB_RSYNC_PATH rsync

COPY scripts /scripts

RUN mkdir ${JB_DEPENDENCIES_DIR}

RUN apt-get -qq update

RUN chmod +x /scripts/install_java.sh && /scripts/install_java.sh
RUN chmod +x /scripts/install_rsync.sh && /scripts/install_rsync.sh
RUN chmod +x /scripts/checkout_repository.sh && /scripts/checkout_repository.sh

CMD cd ${JB_CHECKOUT_DIR} && ./gradlew test
