# Spring Boot Open Telemetry Demo

[![Java CI](https://github.com/mfvanek/spring-boot-open-telemetry-demo/actions/workflows/tests.yml/badge.svg)](https://github.com/mfvanek/spring-boot-open-telemetry-demo/actions/workflows/tests.yml)
[![codecov](https://codecov.io/gh/mfvanek/spring-boot-open-telemetry-demo/graph/badge.svg?token=NUWI02T68G)](https://codecov.io/gh/mfvanek/spring-boot-open-telemetry-demo)

## Local run from IDEA

### Start containers

```shell
docker-compose --file docker/docker-compose-base.yml  --project-name="spring-boot-open-telemetry-demo" up -d
```

#### UI links

* [Jaeger UI](http://localhost:16686)
* [Kafka UI](http://localhost:18080)

### Start applications

```shell
./gradlew spring-boot-2-demo-app:bootRun
```

```shell
./gradlew spring-boot-3-demo-app:bootRun
```

### Make http request

```shell
curl http://localhost:8090/current-time
```

```shell
curl http://localhost:8080/current-time
```

#### Other endpoints

* [Swagger UI SB2](http://localhost:8091/actuator/swagger-ui)
* [Prometheus metrics SB2](http://localhost:8091/actuator/prometheus)
* [Health SB2](http://localhost:8091/actuator/health)

## Run in Docker

### Build images

```shell
./gradlew dockerBuildImage
```

### Run via Compose

```shell
docker-compose --file docker/docker-compose-full.yml  --project-name="spring-boot-open-telemetry-demo" up -d
```

#### UI links

* [Jaeger UI](http://localhost:16686)
* [Kafka UI](http://localhost:18080)

#### How to stop

```shell
docker-compose --project-name="spring-boot-open-telemetry-demo" down
```
