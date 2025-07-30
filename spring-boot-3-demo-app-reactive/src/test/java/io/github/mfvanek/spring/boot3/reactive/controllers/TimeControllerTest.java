/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.controllers;

import io.github.mfvanek.spring.boot3.reactive.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot3.reactive.support.KafkaConsumerUtils;
import io.github.mfvanek.spring.boot3.reactive.support.TestBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimeControllerTest extends TestBase {

    private final BlockingQueue<ConsumerRecord<UUID, String>> consumerRecords = new ArrayBlockingQueue<>(4);
    private KafkaMessageListenerContainer<UUID, String> container;
    @Autowired
    private KafkaProperties kafkaProperties;

    @BeforeAll
    void setUpKafkaConsumer() {
        container = KafkaConsumerUtils.setUpKafkaConsumer(kafkaProperties, consumerRecords);
    }

    @AfterAll
    void tearDownKafkaConsumer() {
        if (container != null) {
            container.stop();
            container = null;
        }
    }

    @BeforeEach
    void cleanUpDatabase() {
        jdbcTemplate.execute("truncate table otel_demo.storage");
    }

    @Order(1)
    @Test
    void spanShouldBeReportedInLogs(@Nonnull final CapturedOutput output) throws InterruptedException {
        stubOkResponse(ParsedDateTime.from(LocalDateTime.now(clock).minusDays(1)));

        final EntityExchangeResult<LocalDateTime> result = webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("current-time")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(LocalDateTime.class)
            .returnResult();
        final String traceId = result.getResponseHeaders().getFirst(TRACE_ID_HEADER_NAME);
        assertThat(traceId).isNotBlank();
        assertThat(result.getResponseBody())
            .isBefore(LocalDateTime.now(clock));
        assertThat(output.getAll())
            .contains("Called method getNow. TraceId = " + traceId)
            .contains("Awaiting acknowledgement from Kafka");

        final ConsumerRecord<UUID, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThatTraceIdPresentInKafkaHeaders(received, traceId);

        awaitStoringIntoDatabase();

        assertThat(output.getAll())
            .contains("Received record: " + received.value() + " with traceId " + traceId)
            .contains("\"tenant.name\":\"ru-a1-private\"");
        final String messageFromDb = namedParameterJdbcTemplate.queryForObject("select message from otel_demo.storage where trace_id = :traceId",
            Map.of("traceId", traceId), String.class);
        assertThat(messageFromDb)
            .isEqualTo(received.value());
    }

    @Order(2)
    @Test
    void spanAndMdcShouldBeReportedWhenRetry(@Nonnull final CapturedOutput output) {
        final String zoneName = stubErrorResponse();

        final EntityExchangeResult<LocalDateTime> result = webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("current-time")
                .build())
            .header("traceparent", "00-38c19768104ab8ae64fabbeed65bbbdf-4cac1747d4e1ee10-01")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(LocalDateTime.class)
            .returnResult();
        final String traceId = result.getResponseHeaders().getFirst(TRACE_ID_HEADER_NAME);
        assertThat(traceId)
            .isEqualTo("38c19768104ab8ae64fabbeed65bbbdf");

        assertThat(output.getAll())
            .containsPattern(String.format(Locale.ROOT,
                ".*\"message\":\"Retrying request to '/%1$s', attempt 1/1 due to error:\"," +
                    "\"logger\":\"io\\.github\\.mfvanek\\.spring\\.boot3\\.reactive\\.service\\.PublicApiService\"," +
                    "\"thread\":\"[^\"]+\",\"level\":\"INFO\",\"stack_trace\":\".+?\"," +
                    "\"traceId\":\"38c19768104ab8ae64fabbeed65bbbdf\",\"spanId\":\"[a-f0-9]+\",\"instance_timezone\":\"%1$s\",\"applicationName\":\"spring-boot-3-demo-app-reactive\"\\}%n", zoneName))
            .containsPattern(String.format(Locale.ROOT,
                ".*\"message\":\"Request to '/%s' failed after 2 attempts.\",\"logger\":\"io\\.github\\.mfvanek\\.spring\\.boot3\\.reactive\\.service\\.PublicApiService\"," +
                    "\"thread\":\"[^\"]+\",\"level\":\"ERROR\"," +
                    "\"traceId\":\"38c19768104ab8ae64fabbeed65bbbdf\",\"spanId\":\"[a-f0-9]+\",\"applicationName\":\"spring-boot-3-demo-app-reactive\"}%n",
                zoneName));
    }

    private long countRecordsInTable() {
        final Long queryResult = jdbcTemplate.queryForObject("select count(*) from otel_demo.storage", Long.class);
        return Objects.requireNonNullElse(queryResult, 0L);
    }

    private void assertThatTraceIdPresentInKafkaHeaders(@Nonnull final ConsumerRecord<UUID, String> received,
                                                        @Nonnull final String expectedTraceId) {
        assertThat(received.value()).startsWith("Current time = ");
        final Header[] headers = received.headers().toArray();
        final List<String> headerNames = Arrays.stream(headers)
            .map(Header::key)
            .toList();
        assertThat(headerNames)
            .hasSize(2)
            .containsExactlyInAnyOrder("traceparent", "b3");
        final List<String> headerValues = Arrays.stream(headers)
            .map(Header::value)
            .map(v -> new String(v, StandardCharsets.UTF_8))
            .toList();
        assertThat(headerValues)
            .hasSameSizeAs(headerNames)
            .allSatisfy(h -> assertThat(h).contains(expectedTraceId));
    }

    private void awaitStoringIntoDatabase() {
        Awaitility
            .await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500L))
            .until(() -> countRecordsInTable() >= 1L);
    }
}
