package io.github.mfvanek.spring.test.controllers;

import java.time.Clock;
import java.util.Optional;

import io.github.mfvanek.spring.test.service.KafkaSendingService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TimeController {

    private final Tracer tracer;
    private final Clock clock;
    private final KafkaSendingService kafkaSendingService;

    // http://localhost:8080/current-time
    @SneakyThrows
    @GetMapping(path = "/current-time")
    public LocalDateTime getNow() {
        final var traceId = Optional.ofNullable(tracer.currentSpan())
                .map(Span::context)
                .map(TraceContext::traceId)
                .orElse(null);
        log.info("Called method getNow. TraceId = {}", traceId);
        final LocalDateTime now = LocalDateTime.now(clock);
        kafkaSendingService.sendNotification("Current time = " + now)
                .thenRun(() -> log.info("Awaiting acknowledgement from Kafka"))
                .get();
        return now;
    }
}
