FROM ubuntu:latest
ENV DEBIAN_FRONTEND="noninteractive"

RUN apt-get update && apt-get install -y wget git curl build-essential maven default-jre

ENV HOME="/home"
RUN mkdir -p /src /home/.m2/repository 

WORKDIR /src
RUN git clone https://github.com/esteinig/japsa && cd japsa/ && git checkout coverage

WORKDIR /src/japsa
RUN echo $HOME && ls /home && bash install_mvn.sh
RUN mvn clean package install -DskipTests=true

RUN ln -s /src/japsa/target/japsacov-1.9.5e.jar /usr/bin/japsacov.jar
RUN echo 'alias jcov="java -cp /usr/bin/japsacov.jar"' >> ~/.bashrc
