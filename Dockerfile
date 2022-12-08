FROM bellsoft/liberica-openjdk-alpine:17.0.5
LABEL maintainer=ivvakhrushev
ARG JAR_FILE
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
