FROM bellsoft/liberica-openjdk-debian:21

RUN apt-get update
RUN apt-get install -y \
    libgomp1 \
    libgcc-s1 \
    libstdc++6 \
    zlib1g \
    libgl1 \
    libglib2.0-0 \
    libgtk2.0-0 \
    libgtk-3-0

USER 1005

VOLUME /config.json
VOLUME /db
#VOLUME /static

ENTRYPOINT ["/frameswork.server/bin/frameswork.server", "/config.json"]

ADD ./build/productionExecutable.tar /static/
ADD ./build/distributions/frameswork.server.tar /
