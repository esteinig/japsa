FROM continuumio/miniconda3

LABEL name="japsa"
LABEL branch="coverage"

RUN apt-get update && apt-get install curl wget build-essential git -y 


RUN conda install openjdk=8

RUN mkdir /src 
WORKDIR /src

RUN git clone https://github.com/esteinig/japsa
RUN cd japsa && make install \
  [INSTALL_DIR=~/.usr/local \] 
  [MXMEM=7000m \] 
  [SERVER=true \] 
  [JLP=/usr/lib/jni]

