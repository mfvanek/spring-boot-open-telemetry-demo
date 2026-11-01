/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */
package io.github.mfvanek.spring.boot.kotlin.test.service

import io.github.mfvanek.spring.boot.kotlin.test.service.dto.CurrentTime
import io.github.mfvanek.spring.boot.kotlin.test.service.dto.ParsedDateTime
import io.github.mfvanek.spring.boot.kotlin.test.service.dto.toLocalDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.util.retry.Retry
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class PublicApiService(
    @param:Value("\${app.retries}") private val retries: Int,
    private val objectMapper: ObjectMapper,
    private val webClient: WebClient
) {
    fun getZonedTime(): LocalDateTime? {
        try {
            val result: ParsedDateTime = getZonedTimeFromWorldTimeApi().datetime
            return result.toLocalDateTime()
        } catch (e: RetriesExhaustedException) {
            logger.warn { "Failed to get response $e" }
        } catch (e: JacksonException) {
            logger.warn { "Failed to convert response $e" }
        }
        return null
    }

    private fun getZonedTimeFromWorldTimeApi(): CurrentTime {
        val zoneName = TimeZone.getDefault().id
        val response = webClient.get()
            .uri(zoneName)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String::class.java)
            .retryWhen(prepareRetry(zoneName))
        return objectMapper.readValue(response.block(), CurrentTime::class.java)
    }

    private fun prepareRetry(zoneName: String): Retry {
        return Retry.fixedDelay(retries.toLong(), Duration.ofSeconds(2))
            .doBeforeRetry { retrySignal: Retry.RetrySignal ->
                withLoggingContext("instance_timezone" to zoneName) {
                    logger.info {
                        "Retrying request to '/$zoneName', attempt ${retrySignal.totalRetries() + 1}/$retries " +
                            "due to error: ${retrySignal.failure()}"
                    }
                }
            }
            .onRetryExhaustedThrow { _, retrySignal: Retry.RetrySignal ->
                logger.error { "Request to '/$zoneName' failed after ${retrySignal.totalRetries() + 1} attempts." }
                RetriesExhaustedException("Retries exhausted", retrySignal.failure())
            }
    }
}
