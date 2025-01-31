/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot2.test.controllers;

import io.github.mfvanek.spring.boot2.test.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot2.test.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot2.test.support.KafkaConsumerUtils;
import io.github.mfvanek.spring.boot2.test.support.TestBase;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.github.mfvanek.spring.boot2.test.filters.TraceIdInResponseServletFilter.TRACE_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimeControllerTest extends TestBase {

    private KafkaMessageListenerContainer<UUID, String> container;
    private final BlockingQueue<ConsumerRecord<UUID, String>> consumerRecords = new LinkedBlockingQueue<>();

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

    @SneakyThrows
    @Test
    void spanShouldBeReportedInLogs(@Nonnull final CapturedOutput output) {
        final String zoneNames = TimeZone.getDefault().getID();
        final ParsedDateTime parsedDateTime = ParsedDateTime.from(LocalDateTime.now(ZoneId.systemDefault()).minusDays(1));
        final CurrentTime currentTime = new CurrentTime(parsedDateTime);
        stubFor(get(urlPathMatching("/" + zoneNames))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString(currentTime))
            ));
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
            .allSatisfy(h -> assertThat(h).contains(traceId));

        Awaitility
            .await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500L))
            .until(() -> countRecordsInTable() >= 1L);
        assertThat(output.getAll())
            .contains("Received record: " + received.value() + " with traceId " + traceId);
        final String messageFromDb = namedParameterJdbcTemplate.queryForObject("select message from otel_demo.storage where trace_id = :traceId",
            Map.of("traceId", traceId), String.class);
        assertThat(messageFromDb)
            .isEqualTo(received.value());
    }

    private long countRecordsInTable() {
        final Long queryResult = jdbcTemplate.queryForObject("select count(*) from otel_demo.storage", Long.class);
        return Objects.requireNonNullElse(queryResult, 0L);
    }

    @SneakyThrows
    @Test
    void mdcValuesShouldBeReportedInLogs(@Nonnull final CapturedOutput output) {
        final String zoneNames = TimeZone.getDefault().getID();
        final ParsedDateTime parsedDateTime = ParsedDateTime.from(LocalDateTime.now(ZoneId.systemDefault()).minusDays(1));
        final CurrentTime currentTime = new CurrentTime(parsedDateTime);
        stubFor(get(urlPathMatching("/" + zoneNames))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString(currentTime))
            ));
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("current-time")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(LocalDateTime.class)
            .returnResult();

        final ConsumerRecord<UUID, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();

        assertThat(output.getAll())
            .contains("\"tenant.name\":\"ru-a1-private\"");
    }

    @SneakyThrows
    @Test
    void spanAndMdcShouldBeReportedWhenRetry(@Nonnull final CapturedOutput output) {
        final String zoneNames = TimeZone.getDefault().getID();
        final RuntimeException exception = new RuntimeException("Retries exhausted");
        stubFor(get(urlPathMatching("/" + zoneNames))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody(objectMapper.writeValueAsString(exception))
            ));

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
        assertThat(output.getAll())
            .contains("Called method getNow. TraceId = " + traceId)
            .contains("Awaiting acknowledgement from Kafka");

        final ConsumerRecord<UUID, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
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
            .allSatisfy(h -> assertThat(h).contains(traceId));

        Awaitility
            .await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500L))
            .until(() -> countRecordsInTable() >= 1L);
        assertThat(output.getAll())
            .contains(
                "Received record: " + received.value() + " with traceId " + traceId,
                "Retrying request to ",
                "Retries exhausted",
                "\"instance_timezone\":\"" + zoneNames + "\""
            );
    }
}
