FROM eclipse-temurin:17.0.7_7-jre-focal
LABEL maintainer=ivvakhrushev
ARG JAR_FILE
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
