FROM ubuntu:16.04


ENV JB_REPO_URL "https://github.com/jetbrains/rsynk.git"
ENV JB_CHECKOUT_DIR "/rsynk"

COPY scripts /scripts

RUN apt-get -qq update && apt-get install -y software-properties-common

RUN chmod +x /scripts/install_java.sh && /scripts/install_java.sh
RUN chmod +x /scripts/checkout_repository.sh && /scripts/checkout_repository.sh

CMD ["$JB_CHECKOUT_DIR/gradlew", "check"]
