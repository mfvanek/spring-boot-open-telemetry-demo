# Spring Boot Open Telemetry Demo

## How to run Jaeger server
```shell
docker run -d --name jaeger \
  -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 6831:6831/udp \
  -p 6832:6832/udp \
  -p 5778:5778 \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  -p 14250:14250 \
  -p 14268:14268 \
  -p 14269:14269 \
  -p 9411:9411 \
  jaegertracing/all-in-one:1.43
```
Open the Jaeger UI on [http://localhost:16686](http://localhost:16686)

### How to stop Jaeger server
```shell
docker rm -f jaeger
```

## Local run
Just run app from IDE and open [http://localhost:8080](http://localhost:8080)

### Swagger
[Swagger UI](http://localhost:8090/actuator/swagger-ui)

### Actuator
[Prometheus metrics](http://localhost:8090/actuator/prometheus)
[Health](http://localhost:8090/actuator/health)

## Run in Docker
### How to build
```shell
./gradlew dockerBuildImage
```

### Docker Compose
#### Start
```shell
docker-compose --project-name="spring-docker-demo" up -d
```
And open [http://localhost:8080](http://localhost:8080)

#### Stop
```shell
docker-compose --project-name="spring-docker-demo" down
```
