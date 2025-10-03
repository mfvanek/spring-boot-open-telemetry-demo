/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.kotlin.test.service

import io.github.mfvanek.db.migrations.common.saver.DbSaver
import io.micrometer.tracing.ScopedSpan
import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.Tracer.SpanInScope
import io.micrometer.tracing.propagation.Propagator
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.support.Acknowledgment
import java.util.UUID

@ExtendWith(MockitoExtension::class)
internal class KafkaReadingServiceTest {
    @Mock
    private lateinit var tracer: Tracer

    @Mock
    private lateinit var propagator: Propagator

    @Mock
    private lateinit var dbSaver: DbSaver

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var record: ConsumerRecord<UUID, String>

    @Mock
    private lateinit var acknowledgment: Acknowledgment

    private lateinit var kafkaReadingService: KafkaReadingService

    @Test
    fun listenAdditionalShouldEndBatchSpanEvenOnException() {
        val headers = Mockito.mock(Headers::class.java)

        Mockito.`when`(record.headers()).thenReturn(headers)

        val spanBuilder = Mockito.mock(Span.Builder::class.java)
        Mockito.`when`(propagator.extract(ArgumentMatchers.any(), ArgumentMatchers.any<Propagator.Getter<Any>>()))
            .thenReturn(spanBuilder)

        val spanFromRecord = Mockito.mock(Span::class.java)
        Mockito.`when`(spanBuilder.name("processing-record-from-kafka")).thenReturn(spanBuilder)
        Mockito.`when`(spanBuilder.start()).thenReturn(spanFromRecord)

        val spanInScope = Mockito.mock(SpanInScope::class.java)
        Mockito.`when`(tracer.withSpan(spanFromRecord)).thenReturn(spanInScope)
        val batchSpan = Mockito.mock(ScopedSpan::class.java)
        Mockito.`when`(tracer.startScopedSpan("batch-processing")).thenReturn(batchSpan)

        kafkaReadingService = KafkaReadingService(tracer, propagator, dbSaver)
        val testException = RuntimeException("DB error")
        Mockito.doThrow(testException).`when`(dbSaver)?.processSingleRecord(record)

        val records = listOf(record)

        Assertions.assertThatThrownBy { kafkaReadingService.listenAdditional(records, acknowledgment) }
            .isSameAs(testException)
        Mockito.verify(tracer).startScopedSpan("batch-processing")
        Mockito.verify(dbSaver).processSingleRecord(record)
        Mockito.verify(acknowledgment, Mockito.never())?.acknowledge()
        Mockito.verify(spanInScope).close()
    }
}
