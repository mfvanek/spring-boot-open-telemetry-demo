/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.service;

import io.github.mfvanek.db.migrations.common.saver.DbSaver;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaReadingService {

    private static final Propagator.Getter<ConsumerRecord<UUID, String>> KAFKA_PROPAGATOR_GETTER = (carrier, key) -> new String(carrier.headers().lastHeader(key).value(), StandardCharsets.UTF_8);

    private final Tracer tracer;
    private final Propagator propagator;
    private final DbSaver dbSaver;

    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void listen(ConsumerRecord<UUID, String> record, Acknowledgment ack) {
        dbSaver.processSingleRecord(record);
        ack.acknowledge();
    }

    @KafkaListener(
        id = "${spring.kafka.consumer.additional-groupId}",
        topics = "${spring.kafka.template.additional-topic}",
        batch = "true"
    )
    public void listenAdditional(List<ConsumerRecord<UUID, String>> records, Acknowledgment ack) {
        final ScopedSpan batchSpan = tracer.startScopedSpan("batch-processing");
        try {
            log.info(
                "Received from Kafka {} records", records.size()
            );
            records.forEach(this::restoreContextAndProcessSingleRecordIfNeed);
            ack.acknowledge();
        } catch (KafkaException e) {
            batchSpan.error(e);
            throw e;
        } finally {
            batchSpan.end();
        }
    }

    private void restoreContextAndProcessSingleRecordIfNeed(ConsumerRecord<UUID, String> record) {
        final Span.Builder builder = propagator.extract(record, KAFKA_PROPAGATOR_GETTER);
        final Span spanFromRecord = builder.name("processing-record-from-kafka").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(spanFromRecord)) {
            dbSaver.processSingleRecord(record);
        } catch (DataAccessException e) {
            spanFromRecord.error(e);
            throw e;
        } finally {
            spanFromRecord.end();
        }
    }
}
