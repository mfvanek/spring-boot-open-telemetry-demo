FROM adoptopenjdk/openjdk11:jdk-11.0.5_10-alpine
LABEL maintainer=ivvakhrushev
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
