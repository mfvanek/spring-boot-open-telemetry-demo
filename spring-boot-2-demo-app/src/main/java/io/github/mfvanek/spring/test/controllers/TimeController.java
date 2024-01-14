package io.github.mfvanek.spring.test.controllers;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
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

    // http://localhost:8080/current-time
    @GetMapping(path = "/current-time")
    public LocalDateTime getNow() {
        final var traceId = Optional.ofNullable(tracer.currentSpan())
                .map(Span::context)
                .map(TraceContext::traceId)
                .orElse(null);
        log.info("Called method getNow. TraceId = {}", traceId);
        return LocalDateTime.now();
    }
}
