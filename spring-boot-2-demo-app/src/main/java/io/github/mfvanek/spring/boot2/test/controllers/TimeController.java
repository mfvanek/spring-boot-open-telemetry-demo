/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot2.test.controllers;

import io.github.mfvanek.spring.boot2.test.service.KafkaSendingService;
import io.github.mfvanek.spring.boot2.test.service.PublicApiService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TimeController {

    private final Tracer tracer;
    private final Clock clock;
    private final KafkaSendingService kafkaSendingService;
    private final PublicApiService publicApiService;

    // http://localhost:8090/current-time
    @SneakyThrows
    @GetMapping(path = "/current-time")
    public LocalDateTime getNow() {
        final String traceId = Optional.ofNullable(tracer.currentSpan())
            .map(Span::context)
            .map(TraceContext::traceId)
            .orElse(null);
        log.info("Called method getNow. TraceId = {}", traceId);
        final LocalDateTime nowFromRemote = publicApiService.getZonedTime();
        final LocalDateTime now = nowFromRemote == null ? LocalDateTime.now(clock) : nowFromRemote;
        kafkaSendingService.sendNotification("Current time = " + now)
            .thenRun(() -> log.info("Awaiting acknowledgement from Kafka"))
            .get();
        return now;
    }
}
