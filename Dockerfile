FROM ubuntu:18.04

RUN apt-get update && apt-get install -y openjdk-11-jdk

COPY bin/japsacov-1.9.5e.jar /usr/local/bin

RUN echo -e '#!/bin/bash\njava -cp /usr/local/bin/japsacov.jar japsa.tools.bio.np.RealtimeSpeciesTypingCmd "$@"' > /usr/bin/jcov-species && chmod +x /usr/bin/jcov-species
