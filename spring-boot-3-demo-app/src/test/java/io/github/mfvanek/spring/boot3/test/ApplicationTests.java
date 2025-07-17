/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test;

import io.github.mfvanek.spring.boot3.test.support.JaegerInitializer;
import io.github.mfvanek.spring.boot3.test.support.TestBase;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTests extends TestBase {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext.containsBean("otlpMeterRegistry"))
            .isFalse();
        assertThat(applicationContext.getBean(ObservationRegistry.class))
            .isNotNull()
            .isInstanceOf(ObservationRegistry.class);
        assertThat(applicationContext.getBean(Tracer.class))
            .isNotNull()
            .isInstanceOf(OtelTracer.class)
            .satisfies(t -> assertThat(t.currentSpan())
                .isNotEqualTo(Span.NOOP));
        assertThat(applicationContext.getBean("otlpGrpcSpanExporter"))
            .isNotNull()
            .isInstanceOf(OtlpGrpcSpanExporter.class)
            .satisfies(e -> assertThat(e.toString())
                .contains(String.format(Locale.ROOT, """
                        OtlpGrpcSpanExporter{exporterName=otlp, type=span, endpoint=http://localhost:%d, \
                        endpointPath=/opentelemetry.proto.collector.trace.v1.TraceService/Export, \
                        timeoutNanos=5000000000, connectTimeoutNanos=2000000000, compressorEncoding=gzip, \
                        headers=Headers{User-Agent=OBFUSCATED}, \
                        retryPolicy=RetryPolicy{maxAttempts=2, initialBackoff=PT1S, maxBackoff=PT5S, backoffMultiplier=1.5, retryExceptionPredicate=null},""",
                    JaegerInitializer.getFirstMappedPort())));
    }

    @Test
    void jdbcQueryTimeoutFromProperties() {
        assertThat(jdbcTemplate.getQueryTimeout())
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Throws exception when query exceeds timeout")
    void exceptionWithLongQuery() {
        assertThatThrownBy(() -> jdbcTemplate.execute("select pg_sleep(1.1);"))
            .isInstanceOf(DataAccessResourceFailureException.class)
            .hasMessageContaining("ERROR: canceling statement due to user request");
    }

    @Test
    @DisplayName("Does not throw exception when query does not exceed timeout")
    void exceptionNotThrownWithNotLongQuery() {
        assertThatNoException().isThrownBy(() -> jdbcTemplate.execute("select pg_sleep(0.9);"));
    }
}
