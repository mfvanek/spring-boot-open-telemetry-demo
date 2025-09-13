/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.service;

import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaReadingService {

    @Value("${app.tenant.name}")
    private String tenantName;
    private final Tracer tracer;
    private final Clock clock;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Propagator propagator;

    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void listen(ConsumerRecord<UUID, String> message, Acknowledgment ack) {
        processSingleRecordIfNeed(message, ack);
    }

    @KafkaListener(
        id = "${spring.kafka.consumer.additional-groupId}",
        topics = "${spring.kafka.template.additional-topic}",
        batch = "true"
    )
    public void listenAdditional(List<ConsumerRecord<UUID, String>> records, Acknowledgment ack) {
        final ScopedSpan batchSpan = tracer.startScopedSpan("batch-processing");
        log.info("current span: {}", tracer.currentSpan());
        try {
            log.info(
                "Received from Kafka {} records", records.size()
            );
            records.forEach(record ->
                restoreContextAndProcessSingleRecordIfNeed(record, ack));
            ack.acknowledge();
        } catch (Exception e) {
            batchSpan.error(e);
            throw e;
        } finally {
            batchSpan.end();
        }
    }

    private void restoreContextAndProcessSingleRecordIfNeed(ConsumerRecord<UUID, String> record, Acknowledgment ack) {
        final Propagator.Getter<ConsumerRecord<UUID, String>> kafkaPropagatorGetter = (carrier, key) -> Arrays.toString(carrier.headers().lastHeader("traceparent").value());
        final Span.Builder builder = propagator.extract(record, kafkaPropagatorGetter);
        final Span spanFromRecord = builder.name("processing-record-from-kafka").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(spanFromRecord)) {
            processSingleRecordIfNeed(record, ack);
        } catch (Exception e) {
            spanFromRecord.error(e);
            throw e;
        } finally {
            spanFromRecord.end();
        }
    }

    private void processSingleRecordIfNeed(ConsumerRecord<UUID, String> message, Acknowledgment ack) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("tenant.name", tenantName)) {
            final Span currentSpan = tracer.currentSpan();
            final String traceId = currentSpan != null ? currentSpan.context().traceId() : "";
            final String spanId = currentSpan != null ? currentSpan.context().spanId() : "";
            log.info("Received record: {} with traceId {}", message.value(), traceId);
            jdbcTemplate.update(
                "insert into otel_demo.storage(message, trace_id, span_id, created_at) values(:msg, :traceId, :currentSpan, :createdAt);",
                Map.ofEntries(
                    Map.entry("msg", message.value()),
                    Map.entry("traceId", traceId),
                    Map.entry("currentSpan", spanId),
                    Map.entry("createdAt", LocalDateTime.now(clock))
                )
            );
            ack.acknowledge();
        }
    }
}
