FROM maven:3.8.1-openjdk-11

RUN apt-get update && apt-get install -y wget git curl

RUN mkdir -p /src /root/.m2/repository 

WORKDIR /src
RUN git clone https://github.com/esteinig/japsa && cd japsa/ && git checkout coverage

WORKDIR /src/japsa
RUN bash install_mvn.sh
RUN mvn clean package install -DskipTests=true

RUN ln -s /src/japsa/target/japsacov-1.9.5e.jar /usr/bin/japsacov.jar
RUN echo 'alias jcov="java -cp /usr/bin/japsacov.jar"' >> ~/.bashrc
