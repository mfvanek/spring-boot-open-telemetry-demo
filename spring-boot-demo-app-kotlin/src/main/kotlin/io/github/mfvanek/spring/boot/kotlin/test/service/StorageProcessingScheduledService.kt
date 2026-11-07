/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.kotlin.test.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

private const val MAX_ROWS_TO_LOG = 20

/**
 * Example of logging inside scheduled operation.
 *
 * Each execution of [processNewRecords] is observed by Spring (see `SchedulingConfig`),
 * so all log messages inside one execution share the same traceId, and different executions have different traceIds.
 */
@Service
class StorageProcessingScheduledService(
    private val jdbcClient: JdbcClient,
    meterRegistry: MeterRegistry,
    @Value("\${app.scheduled-processing.batch-size:100}") private val batchSize: Int
) {
    private val startsCounter = Counter.builder("storage.processing.starts")
        .description("Indicates count of starts of scheduled processNewRecords method")
        .register(meterRegistry)
    private val noDataCounter = Counter.builder("storage.processing.no.data")
        .description("Indicates count of runs of scheduled processNewRecords method when no data for processing found")
        .register(meterRegistry)
    private val lastProcessedId = AtomicLong()

    @Scheduled(
        initialDelayString = "\${app.scheduled-processing.initial-delay:10s}",
        fixedDelayString = "\${app.scheduled-processing.fixed-delay:10s}"
    )
    fun processNewRecords() {
        val startTime = System.nanoTime()
        // Scheduled operations run frequently: use debug level for technical messages in order not to spam logs
        logger.debug { "processNewRecords is started" }
        startsCounter.increment()
        try {
            val data = getDataForProcessing()
            if (data.isEmpty()) {
                logger.debug { "Nothing to do at this time" }
                noDataCounter.increment()
            } else {
                logger.debug { "Found ${data.size} rows to process" }
                logger.trace {
                    val limit = minOf(MAX_ROWS_TO_LOG, data.size)
                    "First $limit rows to process ${data.take(limit)}"
                }
                doSomethingUseful(data)
                lastProcessedId.set(data.last().rowId)
            }
        } finally {
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
            logger.debug { "processNewRecords is finished. Elapsed ${elapsedMillis}ms" }
        }
    }

    private fun doSomethingUseful(data: List<DataForProcess>) {
        val totalSizeOfBodies = data.sumOf { it.body.toByteArray(Charsets.UTF_8).size.toLong() }
        logger.info { "Successfully processed ${data.size} rows with $totalSizeOfBodies bytes" }
    }

    private fun getDataForProcessing(): List<DataForProcess> =
        jdbcClient.sql(
            """
            select id, message, created_at
            from otel_demo.storage
            where id > :minId
            order by id
            limit :batchSize
            """.trimIndent()
        )
            .param("minId", lastProcessedId.get())
            .param("batchSize", batchSize)
            .query { rs, _ ->
                DataForProcess(
                    rowId = rs.getLong("id"),
                    createdAt = rs.getObject("created_at", OffsetDateTime::class.java).toInstant(),
                    body = rs.getString("message")
                )
            }
            .list()

    internal class DataForProcess(val rowId: Long, private val createdAt: Instant, val body: String) {
        override fun toString(): String = "DataForProcess(rowId=$rowId, createdAt=$createdAt, body=$body)"
    }
}
