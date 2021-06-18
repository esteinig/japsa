FROM maven:3.8-openjdk-8-slim


LABEL name="japsa"
LABEL branch="coverage"

RUN apt-get update && apt-get install curl wget build-essential git -y 

RUN mkdir /src 
WORKDIR /src

RUN git clone https://github.com/esteinig/japsa


