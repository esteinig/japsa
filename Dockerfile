FROM ubuntu:18.04

RUN apt-get update && apt-get install -y openjdk-11-jdk

COPY bin/japsacov-1.9.5e.jar /usr/local/bin

ENV JCOV_MEM=8000m
RUN echo -e '#!/bin/bash\njava -Xmx${JCOV_MEM} -cp /usr/local/bin/japsacov-1.9.5e.jar japsa.tools.bio.np.RealtimeSpeciesTypingCmd "$@"' > /usr/bin/jcov-species && chmod +x /usr/bin/jcov-species
