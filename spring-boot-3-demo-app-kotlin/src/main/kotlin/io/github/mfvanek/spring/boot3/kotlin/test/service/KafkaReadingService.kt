/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.kotlin.test.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.tracing.Link
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import io.opentelemetry.context.propagation.TextMapGetter
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
        logger.info { "Received record: ${message.value()} with traceId $traceId" }
        jdbcTemplate.update(
            "insert into otel_demo.storage(message, trace_id, created_at) values(:msg, :traceId, :createdAt);",
            mapOf(
                "msg" to message.value(),
                "traceId" to traceId,
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
        // По умолчанию здесь не будет контекста трассировки. Вся обработка, связанная с трассировкой, должна быть выполнена вручную.
        // Есть несколько вариантов:
        //   1) создать новый спан и контекст трассировки для всех последующих операций;
        //   2) брать контекст трассировки из каждой записи и использовать его на время обработки этой записи;
        //   3) взять контекст трассировки из первой записи и использовать его для всех последующих операций.
        // Рекомендуется использовать комбинацию вариантов 1 и 2.

        // Реализация варианта №1: вручную создать спан, что приведет к созданию контекста трассировки.
        // Этого можно и не делать, но тогда логирование ниже и все последующие операции будут без traceId.
        val batchSpan = tracer.startScopedSpan("batch-processing")
        // val batchSpan = tracer.spanBuilder()
        //    .setParent(tracer.traceContextBuilder().build())
        //   .start()
        logger.info { "current span: ${tracer.currentSpan()}" }
        try {
            logger.info {
                "Received from Kafka ${records.size} records"
            } // Это сообщение будет в логах со своим собственным traceId, если создан спан выше
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
        // Реализация варианта №2.
        // Берём заголовок traceparent из записи и восстанавливаем контекст трассировки на основе него.
        // В результате цепочка спанов продолжится. Все последующие вызовы пойдут с тем же самым traceId.
        // Если в записи из Кафки не будет заголовка traceparent, то будет использоваться текущий контекст трассировки (при его наличии).
        val kafkaPropagatorGetter = Propagator.Getter<ConsumerRecord<UUID, String>> { carrier, _ ->
            carrier.headers().find { it.key() == "traceparent" }?.value()?.decodeToString()
        }

        val builder = propagator.extract(record, kafkaPropagatorGetter)
        // val spanFromRecord = builder.name("processing-record-from-kafka").start()
        val spanFromRecord = builder
            .addLink(Link.NOOP)
            .setParent(tracer.traceContextBuilder().build())
            .start()
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
