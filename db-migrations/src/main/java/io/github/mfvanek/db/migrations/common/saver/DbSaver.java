/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.db.migrations.common.saver;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DbSaver {

    private final String tenantName;
    private final Tracer tracer;
    private final Clock clock;
    private final JdbcClient jdbcClient;

    public void processSingleRecord(ConsumerRecord<UUID, String> record) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("tenant.name", tenantName)) {
            final Span currentSpan = tracer.currentSpan();
            final String traceId = currentSpan != null ? currentSpan.context().traceId() : "";
            final String spanId = currentSpan != null ? currentSpan.context().spanId() : "";
            log.info("Received record: {} with traceId {} spanId {}", record.value(), traceId, spanId);
            jdbcClient.sql("""
                    insert into otel_demo.storage(message, trace_id, span_id, created_at)
                    values(:msg, :traceId, :currentSpan, :createdAt);""")
                .param("msg", record.value())
                .param("traceId", traceId)
                .param("currentSpan", spanId)
                .param("createdAt", LocalDateTime.now(clock))
                .update();
        }
    }
}
