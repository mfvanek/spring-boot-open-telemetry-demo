package io.github.mfvanek.spring.boot3.kotlin.test.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.tracing.Tracer
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class KafkaReadingService(
    @Value("\${app.tenant.name}") private val tenantName: String,
    private val tracer: Tracer,
    private val clock: Clock,
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    @KafkaListener(topics = ["\${spring.kafka.template.default-topic}"])
    fun listen(message: ConsumerRecord<UUID, String>, ack: Acknowledgment) {
        withLoggingContext("tenant.name" to tenantName) {
            val currentSpan = tracer.currentSpan()
            val traceId = currentSpan?.context()?.traceId() ?: ""
            logger.info{"Received record: ${message.value()} with traceId $traceId"}
            jdbcTemplate.update(
                "insert into otel_demo.storage(message, trace_id, created_at) values(:msg, :traceId, :createdAt);",
                mapOf(
                    "msg" to message.value(),
                    "traceId" to traceId,
                    "createdAt" to LocalDateTime.now(clock)
                )
            )
            ack.acknowledge()
        }
    }
}
