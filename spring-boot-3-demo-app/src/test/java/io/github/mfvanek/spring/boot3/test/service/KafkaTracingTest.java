/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mfvanek.spring.boot3.test.support.TestBase;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;

import java.util.Optional;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaTracingTest extends TestBase {

    @MockitoBean
    private ConsumerFactory<UUID, String> consumerFactory;
    @Autowired
    private KafkaReadingService kafkaReadingService;
    @Autowired
    private Tracer tracer;
    @Autowired
    private ObservationRegistry observationRegistry;

    @Test
    @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification = "Justification for suppressing this warning.")
    void errorSpanWhenListenerFails() {
        try (Consumer<UUID, String> mockConsumer = mock(Consumer.class)) {
            when(consumerFactory.createConsumer()).thenReturn(mockConsumer);
            when(mockConsumer.poll(any(Duration.class))).thenReturn(new ConsumerRecords<>(Map.of(
                new TopicPartition("test-topic", 0),
                List.of(createTestConsumerRecord("test-message"))
            )));
            doThrow(new RuntimeException("Commit failed"))
                .when(mockConsumer)
                .commitSync(anyMap());
            final Acknowledgment mockAck = mock(Acknowledgment.class);
            doAnswer(invocation -> {
                mockConsumer.commitSync();
                return null;
            }).when(mockAck).acknowledge();

            final AtomicReference<Throwable> thrownException = new AtomicReference<>();
            final CountDownLatch latch = new CountDownLatch(1);
            Observation.createNotStarted("test", observationRegistry).observe(() -> {
                try {
                    kafkaReadingService.listenAdditional(List.of(createTestConsumerRecord("test-message")), mockAck);
                } catch (Exception e) {
                    thrownException.set(e);
                } finally {
                    latch.countDown();
                }
                assertThat(Objects.requireNonNull(tracer.currentSpan()).error(thrownException.get())).isNotNull();
            });
            assertThat(thrownException.get()).isNotNull();
            assertThat(thrownException.get().getMessage()).contains("Cannot invoke " +
                "\"org.apache.kafka.common.header.Header.value()\" because the return value of " +
                "\"org.apache.kafka.common.header.Headers.lastHeader(String)\" is null");
        }
    }

    private ConsumerRecord<UUID, String> createTestConsumerRecord(String value) {
        final Headers headers = new org.apache.kafka.common.header.internals.RecordHeaders();
        headers.add(new RecordHeader("header", "1".getBytes(StandardCharsets.UTF_8)));

        return new ConsumerRecord<>(
            "test-topic",
            0,
            0L,
            System.currentTimeMillis(),
            TimestampType.CREATE_TIME,
            0L,
            0,
            0,
            UUID.randomUUID(),
            value,
            headers,
            Optional.empty()
        );
    }
}
