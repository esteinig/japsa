FROM continuumio/miniconda3

COPY bin/japsacov-1.9.5e.jar /usr/local/bin

ENV JCOV_MEM=8000m
RUN echo '#!/bin/bash\njava -Xmx${JCOV_MEM} -cp /usr/local/bin/japsacov-1.9.5e.jar japsa.tools.bio.np.RealtimeSpeciesTypingCmd "$@"' > /usr/bin/jcov-species && chmod +x /usr/bin/jcov-species

RUN conda install -c conda-forge -c bioconda minimap2 blast=2.11 openjdk=11

# Default path for minimap2 in JAPSA Coverage module
RUN mkdir -p /sw/minimap2/current && ln -s /opt/conda/bin/minmap2 /sw/minimap2/current/minimap2