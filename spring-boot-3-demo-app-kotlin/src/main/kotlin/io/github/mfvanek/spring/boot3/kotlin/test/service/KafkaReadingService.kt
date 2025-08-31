/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.kotlin.test.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class KafkaReadingService(
    @Value("\${app.tenant.name}") private val tenantName: String,
    private val tracer: Tracer,
    private val clock: Clock,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val propagator: Propagator
) {
    @KafkaListener(topics = ["\${spring.kafka.template.default-topic}"])
    fun listen(message: ConsumerRecord<UUID, String>, ack: Acknowledgment) {
        withLoggingContext("tenant.name" to tenantName) {
            processMessage(message)
            ack.acknowledge()
        }
    }

    private fun processMessage(message: ConsumerRecord<UUID, String>) {
        val currentSpan = tracer.currentSpan()
        val traceId = currentSpan?.context()?.traceId().orEmpty()
        val spanId = currentSpan?.context()?.spanId()
        logger.info { "Received record: ${message.value()} with traceId $traceId" }
        jdbcTemplate.update(
            "insert into otel_demo.storage(message, trace_id, span_id, created_at) values(:msg, :traceId, :currentSpan, :createdAt);",
            mapOf(
                "msg" to message.value(),
                "traceId" to traceId,
                "currentSpan" to spanId,
                "createdAt" to LocalDateTime.now(clock)
            )
        )
    }

    @KafkaListener(
        id = "\${spring.kafka.opentelemetry.additional-consumer-groupId}",
        topics = ["\${spring.kafka.opentelemetry.additional-topic}"],
        batch = "true"
    )
    fun listenAdditional(records: List<ConsumerRecord<UUID, String>>, ack: Acknowledgment) {
        val batchSpan = tracer.startScopedSpan("batch-processing")
        logger.info { "current span: ${tracer.currentSpan()}" }
        try {
            logger.info {
                "Received from Kafka ${records.size} records"
            }
            records.forEach { record ->
                restoreContextAndProcessSingleRecordIfNeed(record, ack)
            }
            ack.acknowledge()
        } catch (e: Throwable) {
            batchSpan.error(e)
            throw e
        } finally {
            batchSpan.end()
        }
    }

    private fun restoreContextAndProcessSingleRecordIfNeed(record: ConsumerRecord<UUID, String>, ack: Acknowledgment) {
        val kafkaPropagatorGetter = Propagator.Getter<ConsumerRecord<UUID, String>> { carrier, _ ->
            carrier.headers().find { it.key() == "traceparent" }?.value()?.decodeToString()
        }
        val builder = propagator.extract(record, kafkaPropagatorGetter)
        val spanFromRecord = builder.name("processing-record-from-kafka").start()
        try {
            tracer.withSpan(spanFromRecord).use {
                processSingleRecordIfNeed(record, ack)
            }
        } catch (e: Throwable) {
            spanFromRecord.error(e)
            throw e
        } finally {
            spanFromRecord.end()
        }
    }

    private fun processSingleRecordIfNeed(record: ConsumerRecord<UUID, String>, ack: Acknowledgment) {
        withLoggingContext("tenant.name" to tenantName) {
            processMessage(record)
            ack.acknowledge()
        }
    }
}
