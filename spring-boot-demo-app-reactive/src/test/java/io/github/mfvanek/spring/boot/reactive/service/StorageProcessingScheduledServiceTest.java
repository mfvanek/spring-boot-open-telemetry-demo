/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.reactive.service;

import io.github.mfvanek.spring.boot.reactive.support.TestBase;
import io.micrometer.core.instrument.MeterRegistry;
import org.awaitility.Awaitility;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class StorageProcessingScheduledServiceTest extends TestBase {

    private static final String LOGGER_NAME = StorageProcessingScheduledService.class.getName();
    private static final String TRACE_ID_PATTERN = "^[0-9a-f]{32}$";
    private static final String SPAN_ID_PATTERN = "^[0-9a-f]{16}$";

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("All log messages inside one execution of scheduled operation should have the same traceId")
    void allLogsInsideScheduledExecutionShouldHaveTheSameTraceId(@NonNull final CapturedOutput output) {
        final String message = "scheduled-" + UUID.randomUUID();
        insertRecord(message);

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                final List<JsonNode> logs = readScheduledServiceLogs(output);
                final List<JsonNode> rowsLogs = logs.stream()
                    .filter(log -> messageOf(log).contains(message))
                    .toList();
                assertThat(rowsLogs)
                    .isNotEmpty()
                    .allSatisfy(rowsLog -> {
                        assertThat(messageOf(rowsLog)).startsWith("First ");
                        final String traceId = traceIdOf(rowsLog);
                        assertThat(traceId).matches(TRACE_ID_PATTERN);
                        assertThat(spanIdOf(rowsLog)).matches(SPAN_ID_PATTERN);

                        final List<String> messagesOfExecution = logs.stream()
                            .filter(log -> traceId.equals(traceIdOf(log)))
                            .map(StorageProcessingScheduledServiceTest::messageOf)
                            .toList();
                        assertThat(messagesOfExecution)
                            .anyMatch(m -> m.startsWith("Found "))
                            .anyMatch(m -> m.equals(messageOf(rowsLog)))
                            .anyMatch(m -> m.startsWith("Successfully processed "))
                            .anyMatch(m -> m.startsWith("processNewRecords is finished. Elapsed "))
                            .noneMatch("Nothing to do at this time"::equals);
                    });
            });
    }

    @Test
    @DisplayName("Each execution of scheduled operation should have its own traceId")
    void eachScheduledExecutionShouldHaveNewTraceId(@NonNull final CapturedOutput output) {
        Awaitility
            .await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                final List<String> traceIdsOfFinishedExecutions = readScheduledServiceLogs(output).stream()
                    .filter(log -> messageOf(log).startsWith("processNewRecords is finished"))
                    .map(StorageProcessingScheduledServiceTest::traceIdOf)
                    .toList();
                assertThat(traceIdsOfFinishedExecutions)
                    .hasSizeGreaterThanOrEqualTo(3)
                    .allMatch(traceId -> traceId.matches(TRACE_ID_PATTERN))
                    .doesNotHaveDuplicates();
            });

        final List<String> traceIdsOfStartedExecutions = readScheduledServiceLogs(output).stream()
            .filter(log -> "processNewRecords is started".equals(messageOf(log)))
            .map(StorageProcessingScheduledServiceTest::traceIdOf)
            .toList();
        assertThat(traceIdsOfStartedExecutions)
            .isNotEmpty()
            .allMatch(traceId -> traceId.matches(TRACE_ID_PATTERN))
            .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Scheduled operation should be observed and should report custom metrics")
    void scheduledExecutionsShouldBeObserved() {
        Awaitility
            .await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                assertThat(meterRegistry.get("tasks.scheduled.execution")
                    .tag("code.function", "processNewRecords")
                    .timer()
                    .count())
                    .isPositive();
                assertThat(meterRegistry.get("storage.processing.starts").counter().count())
                    .isPositive();
                assertThat(meterRegistry.get("storage.processing.no.data").counter().count())
                    .isPositive();
            });
    }

    private void insertRecord(@NonNull final String message) {
        jdbcTemplate.update(
            "insert into otel_demo.storage(message, trace_id, span_id, created_at) values (?, ?, ?, now())",
            message, UUID.randomUUID().toString(), UUID.randomUUID().toString()
        );
    }

    @NonNull
    private List<JsonNode> readScheduledServiceLogs(@NonNull final CapturedOutput output) {
        return output.getAll().lines()
            .filter(line -> line.startsWith("{") && line.contains(LOGGER_NAME))
            // OutputCapture may contain the same output several times when captures are nested (class and method level)
            .distinct()
            .flatMap(this::parseJson)
            .filter(log -> LOGGER_NAME.equals(log.path("logger").asString()))
            .toList();
    }

    @NonNull
    private Stream<JsonNode> parseJson(@NonNull final String line) {
        try {
            return Stream.of(objectMapper.readTree(line));
        } catch (JacksonException ignored) {
            // lines from different threads can be interleaved
            return Stream.empty();
        }
    }

    @NonNull
    private static String messageOf(@NonNull final JsonNode log) {
        return log.path("message").asString();
    }

    @NonNull
    private static String traceIdOf(@NonNull final JsonNode log) {
        return log.path("traceId").asString();
    }

    @NonNull
    private static String spanIdOf(@NonNull final JsonNode log) {
        return log.path("spanId").asString();
    }
}
