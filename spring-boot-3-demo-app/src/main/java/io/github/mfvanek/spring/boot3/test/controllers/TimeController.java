package io.github.mfvanek.spring.boot3.test.controllers;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TimeController {

    private final Tracer tracer;

    // http://localhost:8080/current-time
    @GetMapping(path = "/current-time")
    public LocalDateTime getNow() {
        log.info("tracer {}", tracer);
        final var traceId = Optional.ofNullable(tracer.currentSpan())
                .map(Span::context)
                .map(TraceContext::traceId)
                .orElse(null);
        log.info("Called method getNow. TraceId = {}", traceId);
        return LocalDateTime.now();
    }
}
