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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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

    // http://localhost:8081/current-time
    @GetMapping(path = "/current-time")
    public Mono<LocalDateTime> getNow() {
        log.trace("tracer {}", tracer);
        return Mono.justOrEmpty(
                Optional.ofNullable(tracer.currentSpan())
                    .map(Span::context)
                    .map(TraceContext::traceId)
                    .orElse(null)
            )
            .doOnNext(traceId -> log.info("Called method getNow. TraceId = {}", traceId))
            .then(publicApiService.getZonedTime())
            .defaultIfEmpty(LocalDateTime.now(clock))
            .flatMap(now -> kafkaSendingService.sendNotification("Current time = " + now)
                .doOnSuccess(v -> log.info("Awaiting acknowledgement from Kafka"))
                .thenReturn(now)
            )
            .flatMap(now -> kafkaSendingService.sendNotificationToOtherTopic("Current time = " + now)
                .doOnSuccess(v -> log.info("Awaiting acknowledgement from Kafka with batch"))
                .thenReturn(now));
    }
}
