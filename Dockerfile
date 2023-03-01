FROM openjdk:11

USER root
WORKDIR /app
COPY ./target/emtest-*.jar emtest.jar
RUN jar -xf emtest.jar

RUN locale -a
RUN env

ENTRYPOINT ["java", "-jar", "emtest.jar"]

EXPOSE 8080