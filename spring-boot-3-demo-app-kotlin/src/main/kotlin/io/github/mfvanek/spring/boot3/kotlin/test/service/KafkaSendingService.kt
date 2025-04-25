package io.github.mfvanek.spring.boot3.kotlin.test.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import java.util.UUID
import java.util.concurrent.CompletableFuture
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class KafkaSendingService(
    @Value("\${app.tenant.name}") private val tenantName: String,
    private val kafkaTemplate: KafkaTemplate<UUID, String>
) {
    fun sendNotification(message: String): CompletableFuture<SendResult<UUID, String>> {
        withLoggingContext("tenant.name" to tenantName) {
            logger.info{"Sending message \"$message\" to Kafka"}
            return kafkaTemplate.sendDefault(UUID.randomUUID(), message)
        }
    }
}
