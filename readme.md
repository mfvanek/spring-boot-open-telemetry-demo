## How to run Jaeger server
```bash
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
  jaegertracing/all-in-one:1.39
```
Open the Jaeger UI on [http://localhost:16686](http://localhost:16686)

## Local run
Just run app from IDE and open [http://localhost:8080](http://localhost:8080)

### Swagger
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Actuator
[Prometheus metrics](http://localhost:8080/actuator/prometheus)

## Run in Docker

### How to build
`docker build --build-arg JAR_FILE=build/libs/spring.test-0.0.2-SNAPSHOT.jar -t io.github.mfvanek/spring_test .`

### How to run
`docker run --rm --name springDemoApp --env SPRING_PROFILES_ACTIVE=docker -p 8080:8080 io.github.mfvanek/spring_test`  
And open [http://localhost:8080](http://localhost:8080)
