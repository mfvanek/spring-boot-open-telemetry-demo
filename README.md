# Spring Boot OpenTelemetry Demo

A set of small Spring Boot applications to demonstrate OpenTelemetry capabilities and nuances

[![Java CI](https://github.com/mfvanek/spring-boot-open-telemetry-demo/actions/workflows/tests.yml/badge.svg)](https://github.com/mfvanek/spring-boot-open-telemetry-demo/actions/workflows/tests.yml)
[![codecov](https://codecov.io/gh/mfvanek/spring-boot-open-telemetry-demo/graph/badge.svg?token=NUWI02T68G)](https://codecov.io/gh/mfvanek/spring-boot-open-telemetry-demo)

## Spring Cloud Sleuth example

- If you are looking for a **Spring Boot 2.7.x** example with [Spring Cloud Sleuth](https://spring.io/projects/spring-cloud-sleuth), take a look at version [0.4.1](https://github.com/mfvanek/spring-boot-open-telemetry-demo/releases/tag/v.0.4.1)
- If you are looking for a **Spring Boot 3.5.x** example, take a look at version [0.5.1](https://github.com/mfvanek/spring-boot-open-telemetry-demo/releases/tag/v.0.5.1)

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
./gradlew spring-boot-demo-app-kotlin:bootRun
```

```shell
./gradlew spring-boot-demo-app:bootRun
```

```shell
./gradlew spring-boot-demo-app-reactive:bootRun
```

### Make http request

```shell
curl http://localhost:8090/current-time
```

```shell
curl http://localhost:8080/current-time
```

```shell
curl http://localhost:8086/current-time
```

#### Other endpoints

| Application                   | Swagger UI                                           | Prometheus metrics                                   | Health                                           |
|-------------------------------|------------------------------------------------------|------------------------------------------------------|--------------------------------------------------|
| spring-boot-demo-app          | [link](http://localhost:8085/actuator/swagger-ui) | [link](http://localhost:8085/actuator/prometheus) | [link](http://localhost:8085/actuator/health) |
| spring-boot-demo-app-kotlin   | [link](http://localhost:8091/actuator/swagger-ui) | [link](http://localhost:8091/actuator/prometheus) | [link](http://localhost:8091/actuator/health) |
| spring-boot-demo-app-reactive | [link](http://localhost:8087/actuator/swagger-ui) | [link](http://localhost:8087/actuator/prometheus) | [link](http://localhost:8087/actuator/health) |

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
