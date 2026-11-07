/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.kotlin.test.service

import io.github.mfvanek.spring.boot.kotlin.test.support.TestBase
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.util.*

private val LOGGER_NAME = StorageProcessingScheduledService::class.java.name
private val TRACE_ID_PATTERN = Regex("^[0-9a-f]{32}$")
private val SPAN_ID_PATTERN = Regex("^[0-9a-f]{16}$")
private const val MIN_EXECUTIONS = 3

@ExtendWith(OutputCaptureExtension::class)
class StorageProcessingScheduledServiceTest : TestBase() {

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Test
    @DisplayName("All log messages inside one execution of scheduled operation should have the same traceId")
    fun allLogsInsideScheduledExecutionShouldHaveTheSameTraceId(output: CapturedOutput) {
        val message = "scheduled-${UUID.randomUUID()}"
        insertRecord(message)

        awaitUntilAsserted {
            val logs = readScheduledServiceLogs(output)
            val rowsLogs = logs.filter { it.message().contains(message) }
            assertThat(rowsLogs)
                .isNotEmpty()
                .allSatisfy { rowsLog ->
                    assertThat(rowsLog.message()).startsWith("First ")
                    val traceId = rowsLog.traceId()
                    assertThat(traceId).matches(TRACE_ID_PATTERN.toPattern())
                    assertThat(rowsLog.spanId()).matches(SPAN_ID_PATTERN.toPattern())

                    val messagesOfExecution = logs
                        .filter { it.traceId() == traceId }
                        .map { it.message() }
                    assertThat(messagesOfExecution)
                        .anyMatch { it.startsWith("Found ") }
                        .anyMatch { it == rowsLog.message() }
                        .anyMatch { it.startsWith("Successfully processed ") }
                        .anyMatch { it.startsWith("processNewRecords is finished. Elapsed ") }
                        .noneMatch { it == "Nothing to do at this time" }
                }
        }
    }

    @Test
    @DisplayName("Each execution of scheduled operation should have its own traceId")
    fun eachScheduledExecutionShouldHaveNewTraceId(output: CapturedOutput) {
        awaitUntilAsserted {
            val traceIdsOfFinishedExecutions = readScheduledServiceLogs(output)
                .filter { it.message().startsWith("processNewRecords is finished") }
                .map { it.traceId() }
            assertThat(traceIdsOfFinishedExecutions)
                .hasSizeGreaterThanOrEqualTo(MIN_EXECUTIONS)
                .allMatch { TRACE_ID_PATTERN.matches(it) }
                .doesNotHaveDuplicates()
        }

        val traceIdsOfStartedExecutions = readScheduledServiceLogs(output)
            .filter { it.message() == "processNewRecords is started" }
            .map { it.traceId() }
        assertThat(traceIdsOfStartedExecutions)
            .isNotEmpty()
            .allMatch { TRACE_ID_PATTERN.matches(it) }
            .doesNotHaveDuplicates()
    }

    @Test
    @DisplayName("Scheduled operation should be observed and should report custom metrics")
    fun scheduledExecutionsShouldBeObserved() {
        awaitUntilAsserted {
            assertThat(
                meterRegistry.get("tasks.scheduled.execution")
                    .tag("code.function", "processNewRecords")
                    .timer()
                    .count()
            ).isPositive()
            assertThat(meterRegistry.get("storage.processing.starts").counter().count())
                .isPositive()
            assertThat(meterRegistry.get("storage.processing.no.data").counter().count())
                .isPositive()
        }
    }

    private fun insertRecord(message: String) {
        jdbcTemplate.update(
            "insert into otel_demo.storage(message, trace_id, span_id, created_at) values (?, ?, ?, now())",
            message,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        )
    }

    private fun readScheduledServiceLogs(output: CapturedOutput): List<JsonNode> =
        output.all.lineSequence()
            .filter { it.startsWith("{") && it.contains(LOGGER_NAME) }
            // OutputCapture may contain the same output several times when captures are nested (class and method level)
            .distinct()
            .mapNotNull { parseJson(it) }
            .filter { it.path("logger").asString() == LOGGER_NAME }
            .toList()

    private fun parseJson(line: String): JsonNode? =
        try {
            mapper.readTree(line)
        } catch (_: JacksonException) {
            // lines from different threads can be interleaved
            null
        }

    private fun awaitUntilAsserted(assertion: () -> Unit) {
        Awaitility
            .await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted { assertion() }
    }
}

private fun JsonNode.message(): String = path("message").asString()

private fun JsonNode.traceId(): String = path("traceId").asString()

private fun JsonNode.spanId(): String = path("spanId").asString()
