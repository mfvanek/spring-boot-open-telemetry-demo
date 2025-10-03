/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.mfvanek.db.migrations.common.saver.DbSaver;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Tracer.SpanInScope;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaReadingServiceTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Propagator propagator;

    @Mock
    private DbSaver dbSaver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConsumerRecord<UUID, String> record;

    @Mock
    private Acknowledgment acknowledgment;

    private KafkaReadingService kafkaReadingService;

    @Test
    @SuppressWarnings("PMD.CloseResource")
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")// to suppress warning when calling propagator.extract(any(), any())
    void listenAdditionalShouldEndBatchSpanEvenOnException() {
        final Headers headers = mock(Headers.class);

        when(record.headers()).thenReturn(headers);

        final Span.Builder spanBuilder = mock(Span.Builder.class);
        when(propagator.extract(any(), any())).thenReturn(spanBuilder);

        final Span spanFromRecord = mock(Span.class);
        when(spanBuilder.name("processing-record-from-kafka")).thenReturn(spanBuilder);
        when(spanBuilder.start()).thenReturn(spanFromRecord);

        final SpanInScope spanInScope = mock(SpanInScope.class);
        when(tracer.withSpan(spanFromRecord)).thenReturn(spanInScope);
        final ScopedSpan batchSpan = mock(ScopedSpan.class);
        when(tracer.startScopedSpan("batch-processing")).thenReturn(batchSpan);

        kafkaReadingService = new KafkaReadingService(tracer, propagator, dbSaver);
        final RuntimeException testException = new RuntimeException("DB error");
        doThrow(testException).when(dbSaver).processSingleRecord(record);

        final List<ConsumerRecord<UUID, String>> records = List.of(record);

        assertThatThrownBy(() -> kafkaReadingService.listenAdditional(records, acknowledgment))
            .isSameAs(testException);
        verify(tracer).startScopedSpan("batch-processing");
        verify(dbSaver).processSingleRecord(record);
        verify(acknowledgment, never()).acknowledge();
        verify(spanInScope).close();
    }
}
