/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.kotlin.test.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

@Service
class KafkaSendingService(
    @Value("\${app.tenant.name}") private val tenantName: String,
    private val kafkaTemplate: KafkaTemplate<UUID, String>
) {
    fun sendNotification(message: String): CompletableFuture<SendResult<UUID, String>> {
        withLoggingContext("tenant.name" to tenantName) {
            logger.info { "Sending message \"$message\" to Kafka" }
            return kafkaTemplate.sendDefault(UUID.randomUUID(), message)
        }
    }
}
