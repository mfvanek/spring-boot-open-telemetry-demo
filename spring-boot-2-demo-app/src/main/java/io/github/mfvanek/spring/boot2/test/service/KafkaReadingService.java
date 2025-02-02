/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot2.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
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

    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void listen(ConsumerRecord<UUID, String> message, Acknowledgment ack) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("tenant.name", tenantName)) {
            final Span currentSpan = tracer.currentSpan();
            final String traceId = currentSpan != null ? currentSpan.context().traceId() : "";
            log.info("Received record: {} with traceId {}", message.value(), traceId);
            jdbcTemplate.update("insert into otel_demo.storage(message, trace_id, created_at) values(:msg, :traceId, :createdAt);",
                Map.ofEntries(
                    Map.entry("msg", message.value()),
                    Map.entry("traceId", traceId),
                    Map.entry("createdAt", LocalDateTime.now(clock))
                )
            );
        }
        ack.acknowledge();
    }
}
