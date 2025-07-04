/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.controllers;

import io.github.mfvanek.spring.boot3.reactive.service.KafkaSendingService;
import io.github.mfvanek.spring.boot3.reactive.service.PublicApiService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TimeController {

    private final Tracer tracer;
    private final Clock clock;
    private final KafkaSendingService kafkaSendingService;
    private final PublicApiService publicApiService;

    // http://localhost:8080/current-time
    @SneakyThrows
    @GetMapping(path = "/current-time")
    public Mono<LocalDateTime> getNow() {
        log.trace("tracer {}", tracer);
        final String traceId = Optional.ofNullable(tracer.currentSpan())
            .map(Span::context)
            .map(TraceContext::traceId)
            .orElse(null);
        log.info("Called method getNow. TraceId = {}", traceId);
        return Objects.requireNonNull(Objects.requireNonNull(publicApiService
                .getZonedTime())
            .map(it -> {
                if (it.equals(LocalDateTime.MIN)) {
                    final LocalDateTime localDateTime = LocalDateTime.now(clock);
                    sendWithKafka(localDateTime);
                    return localDateTime;
                } else {
                    sendWithKafka(it);
                    return it;
                }
            }));
    }

    private void sendWithKafka(LocalDateTime localDateTime) {
        try {
            kafkaSendingService.sendNotification("Current time = " + localDateTime)
                .thenRun(() -> log.info("Awaiting acknowledgement from Kafka"))
                .get();
        } catch (InterruptedException | ExecutionException e) {
            log.info("error ", e);
        }
    }
}
