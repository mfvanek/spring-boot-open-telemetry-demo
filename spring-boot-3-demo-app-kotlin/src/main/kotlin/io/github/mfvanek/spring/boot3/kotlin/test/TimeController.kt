/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */
package io.github.mfvanek.spring.boot3.kotlin.test

import io.github.mfvanek.spring.boot3.kotlin.test.service.KafkaSendingService
import io.github.mfvanek.spring.boot3.kotlin.test.service.PublicApiService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.tracing.Tracer
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class TimeController(
    private val tracer: Tracer,
    private val clock: Clock,
    private val kafkaSendingService: KafkaSendingService,
    private val publicApiService: PublicApiService
) {

    @GetMapping(path = ["/current-time"])
    fun getNow(): LocalDateTime {
        logger.trace { "tracer $tracer" }
        val traceId = tracer.currentSpan()?.context()?.traceId()
        logger.info { "Called method getNow. TraceId = $traceId" }
        val nowFromRemote = publicApiService.getZonedTime()
        val now = nowFromRemote ?: LocalDateTime.now(clock)
        kafkaSendingService.sendNotification("Current time = $now")
            .thenRun { logger.info{"Awaiting acknowledgement from Kafka"} }
            .get()
        return now
    }
}
