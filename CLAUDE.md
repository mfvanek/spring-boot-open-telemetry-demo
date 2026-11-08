# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project purpose

A set of small Spring Boot 4 applications that demonstrate OpenTelemetry tracing (via Micrometer Tracing + OTel bridge, exported over OTLP/gRPC to Jaeger) across HTTP, WebClient, Kafka, JDBC and `@Scheduled` jobs. The point of most code here is to show *how trace context flows*, so preserve tracing behaviour (observation flags, context propagation, span handling) when changing things.

## Build & test commands

Java 25 toolchain is required (CI uses Temurin 25). Gradle wrapper only.

```shell
./gradlew build                                   # full build: compile, static analysis, tests, coverage verification (what CI runs)
./gradlew :spring-boot-demo-app:test              # tests for one module
./gradlew :spring-boot-demo-app:test --tests "io.github.mfvanek.spring.boot.test.service.KafkaTracingTest"
./gradlew :spring-boot-demo-app:test --tests "*TimeControllerTest.someMethod"
./gradlew :spring-boot-demo-app-kotlin:detekt     # Kotlin lint (autoCorrect is enabled)
./gradlew dockerBuildImage                        # build Docker images for all apps
./gradlew dependencyUpdates                       # check for newer stable dependency versions
./gradlew spring-boot-demo-app:bootRun            # run locally (needs docker/docker-compose-base.yml up)
```

- Tests need a running Docker daemon: they start Kafka, PostgreSQL and Jaeger via Testcontainers.
- `test` depends on all static analysis tasks, so a lint violation fails the test run. Java modules: Checkstyle, PMD, SpotBugs (max effort, low confidence threshold), Error Prone, forbidden-apis. Kotlin module: detekt + forbidden-apis. Configs live in `config/`.
- `test` is finalized by JaCoCo report + coverage verification with strict limits (e.g. zero missed classes, ≥93% instruction coverage by default; per-module overrides in each `build.gradle.kts`). New code without tests will usually fail `check`.
- Test retry is enabled (up to 2 retries), so flaky failures may be masked locally — read the report.
- forbidden-apis bans e.g. `Collections.emptyList/singletonList/unmodifiable*`, `Arrays.asList`, `System.out`, and `org.springframework.lang.Nullable` (use `org.jspecify.annotations.Nullable` / `List.of()` etc.). See `config/forbidden-apis/forbidden-apis.txt`.
- Every source file starts with the Apache 2.0 copyright header (`Copyright (c) 2020-2026. Ivan Vakhrushev and others.`) — copy it from an existing file.

## Local environment

```shell
docker-compose --file docker/docker-compose-base.yml --project-name="spring-boot-open-telemetry-demo" up -d   # Jaeger, Kafka (SASL/PLAIN), Kafka UI, Postgres
docker-compose --file docker/docker-compose-full.yml --project-name="spring-boot-open-telemetry-demo" up -d   # + the apps (images from dockerBuildImage)
```

Jaeger UI: http://localhost:16686, Kafka UI: http://localhost:18080, Postgres on host port 6432, Kafka on 9092. The `docker` Spring profile switches hosts to the compose service names.

| App | HTTP port | Management (actuator/swagger) port |
|---|---|---|
| spring-boot-demo-app (Java, MVC) | 8080 | 8085 |
| spring-boot-demo-app-kotlin (Kotlin, MVC) | 8090 | 8091 |
| spring-boot-demo-app-reactive (Java, WebFlux) | 8086 | 8087 |

These ports are fixed: keep `application.yml`, `docker/docker-compose-full.yml`, the README and the URL comments in controllers in sync with this table. Never use port 8081 — it is blocked on some MacBooks.

## Architecture

Gradle multi-module build; convention plugins in `buildSrc/src/main/kotlin` (`sb-ot-demo.java-conventions`, `sb-ot-demo.kotlin-conventions`, `sb-ot-demo.java-compile`, `sb-ot-demo.jacoco-rules`, `sb-ot-demo.forbidden-apis`, `sb-ot-demo.docker`). Versions are in `gradle/libs.versions.toml` and `common-internal-bom` (internal platform for test libs, Liquibase, Postgres, Hikari, etc.).

Library modules:
- `core` — shared tiny types (`RetriesExhaustedException`).
- `db-migrations` — Liquibase changelog (`db/changelog/db.changelog-master.yaml`, schema `otel_demo`, table `storage`) plus `DbSaver`, which persists a Kafka record together with the current traceId/spanId. Used by all apps.
- `test-initializers` — Testcontainers `ApplicationContextInitializer`s (`KafkaInitializer`, `PostgresInitializer`, `JaegerInitializer`) using static singleton containers shared across test contexts.

The three apps are **parallel implementations of the same scenario** with the same package layout (`config`, `controllers`, `filters`, `service`, `service/dto`). A change to the demo flow usually has to be mirrored in all three (Java MVC, Kotlin MVC, reactive WebFlux), including their tests.

Demo flow (`GET /current-time`):
1. `TimeController` logs the current traceId, calls `PublicApiService` (WebClient → worldtimeapi.org, with Reactor retry; falls back to local `Clock` on failure).
2. Sends the result to two Kafka topics via `KafkaSendingService` (`KafkaTemplate` with `observation-enabled: true`, so trace headers are injected).
3. `KafkaReadingService` consumes: the single-record listener relies on Spring Kafka observation; the **batch** listener on the additional topic manually restores each record's context with `Propagator.extract(...)` and creates a span per record — the key example of propagation in batch consumers.
4. `DbSaver` writes the message with trace/span ids into Postgres.
5. `StorageProcessingScheduledService` (`@Scheduled`, observed via `SchedulingConfig`) reads new rows — each execution gets its own traceId.

Other tracing pieces: `TraceIdInResponseServletFilter` / `TraceIdInResponseReactiveFilter` put the traceId in the `X-TraceId` response header (must stay the same in all apps); `OpenTelemetryConfig` customizes the OTLP gRPC exporter retry policy; propagation is configured for both B3 and W3C (`management.tracing.propagation.type`); the reactive app relies on `spring.reactor.context-propagation: auto`.

## Testing conventions

- Integration tests extend `support/TestBase` in each app: `@SpringBootTest(RANDOM_PORT)` with profile `test`, `@AutoConfigureTracing`/`@AutoConfigureMetrics`, Testcontainers initializers, WireMock (`@EnableWireMock`) stubbing the external time API (`app.external-base-url` points to WireMock in `application-test.yml`), and a fixed `Clock`.
- Spans are asserted through an in-memory exporter wired in `support/SpanExporterConfiguration` (opentelemetry-sdk-testing).
- `DatabaseStructureStaticAnalysisTest` runs pg-index-health checks against the migrated schema — new DB objects must pass them.
- The reactive app tests use BlockHound (`-XX:+AllowRedefinitionToAddDeleteMethods` is set) — blocking calls on non-blocking threads fail tests.
