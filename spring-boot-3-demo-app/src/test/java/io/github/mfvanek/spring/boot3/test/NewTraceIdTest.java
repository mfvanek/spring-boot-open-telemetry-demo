/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test;

import io.github.mfvanek.spring.boot3.test.support.TestBase;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"PMD.AvoidCatchingThrowable", "PMD.ExceptionAsFlowControl", "checkstyle:NestedTryDepth", "checkstyle:IllegalCatch"})
class NewTraceIdTest extends TestBase {

    @Autowired
    private Tracer tracer;

    @Test
    @DisplayName("Demonstration of how to create a new traceId using the span API")
    void canCreateNewTraceIdViaSpan() {
        final ScopedSpan firstSpan = tracer.startScopedSpan("first");
        try {
            final String previousTraceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();
            assertThat(previousTraceId)
                .isEqualTo(firstSpan.context().traceId());

            try (Tracer.SpanInScope ignored = tracer.withSpan(null)) {
                final Span newSpan = tracer.nextSpan().name("new");
                try (Tracer.SpanInScope ignored2 = tracer.withSpan(newSpan.start())) {
                    final String newTraceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();
                    assertThat(newTraceId)
                        .isNotEqualTo(previousTraceId);
                } catch (Throwable e) {
                    newSpan.error(e);
                    throw e;
                } finally {
                    newSpan.end();
                }
            }

            final String lastTraceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();
            assertThat(lastTraceId)
                .isEqualTo(previousTraceId);
        } catch (Throwable e) {
            firstSpan.error(e);
            throw e;
        } finally {
            firstSpan.end();
        }
    }

    @Test
    @DisplayName("Demonstration of creating a new traceId using the API traceContext")
    void canCreateNewTraceIdViaContext() {
        final ScopedSpan firstSpan = tracer.startScopedSpan("first");
        try {
            final String previousTraceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();
            assertThat(previousTraceId)
                .isEqualTo(firstSpan.context().traceId());

            try (Tracer.SpanInScope ignored = tracer.withSpan(null)) {
                final Span newSpan = tracer.nextSpan();
                final TraceContext traceContext = tracer.traceContextBuilder()
                    .traceId(newSpan.context().traceId())
                    .spanId(newSpan.context().spanId())
                    .sampled(Boolean.TRUE) // Important!
                    .build();

                try (CurrentTraceContext.Scope unused = tracer.currentTraceContext().newScope(traceContext)) {
                    final String newTraceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();
                    assertThat(newTraceId)
                        .isNotEqualTo(previousTraceId);
                }
            }

            final String lastTraceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();
            assertThat(lastTraceId)
                .isEqualTo(previousTraceId);
        } catch (Throwable e) {
            firstSpan.error(e);
            throw e;
        } finally {
            firstSpan.end();
        }
    }
}
