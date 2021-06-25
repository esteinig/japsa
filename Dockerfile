FROM continuumio/miniconda3

RUN apt-get update && apt-get install git
RUN git clone https://github.com/esteinig/japsa && cd japsa && git checkout coverage && mv jar/japsacov-1.9.5e.jar /usr/local/bin
RUN conda install -c conda-forge -c bioconda minimap2 blast=2.11 openjdk=11
 
# Default path for minimap2 in JAPSA Coverage module
RUN mkdir -p /sw/minimap2/current && ln -s /opt/conda/bin/minmap2 /sw/minimap2/current/minimap2