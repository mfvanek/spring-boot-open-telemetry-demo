package io.github.mfvanek.spring.boot3.kotlin.test.service

import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

@Service
class KafkaSendingService(
    @Value("\${app.tenant.name}") private val tenantName: String,
    private val kafkaTemplate: KafkaTemplate<UUID, String>
) {
    fun sendNotification(message: String): CompletableFuture<SendResult<UUID, String>> {
        MDC.putCloseable("tenant.name", tenantName).use {
            logger.info("Sending message \"{}\" to Kafka", message)
            return kafkaTemplate.sendDefault(UUID.randomUUID(), message)
        }
    }
}
