package io.github.mfvanek.spring.boot3.test.controllers;

import io.github.mfvanek.spring.boot3.test.service.KafkaSendingService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

    // http://localhost:8080/current-time
    @SneakyThrows
    @GetMapping(path = "/current-time")
    public LocalDateTime getNow() {
        log.trace("tracer {}", tracer);
        final String traceId = Optional.ofNullable(tracer.currentSpan())
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
