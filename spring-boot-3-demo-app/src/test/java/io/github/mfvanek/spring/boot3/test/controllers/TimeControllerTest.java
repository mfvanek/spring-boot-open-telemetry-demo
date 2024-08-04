package io.github.mfvanek.spring.boot3.test.controllers;

import io.github.mfvanek.spring.boot3.test.support.KafkaConsumerUtils;
import io.github.mfvanek.spring.boot3.test.support.TestBase;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import static io.github.mfvanek.spring.boot3.test.filters.TraceIdInResponseServletFilter.TRACE_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimeControllerTest extends TestBase {

    private KafkaMessageListenerContainer<UUID, String> container;
    private final BlockingQueue<ConsumerRecord<UUID, String>> consumerRecords = new LinkedBlockingQueue<>();

    @Autowired
    private KafkaProperties kafkaProperties;
    @Autowired
    private Clock clock;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

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
        jdbcTemplate.getJdbcTemplate().execute("truncate table otel_demo.storage");
    }

    @SneakyThrows
    @Test
    void spanShouldBeReportedInLogs(@Nonnull final CapturedOutput output) {
        final var result = webTestClient.get()
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

        final var received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.value()).startsWith("Current time = ");
        final Header[] headers = received.headers().toArray();
        final var headerNames = Arrays.stream(headers)
                .map(Header::key)
                .toList();
        assertThat(headerNames)
                .hasSize(2)
                .containsExactlyInAnyOrder("traceparent", "b3");
        final var headerValues = Arrays.stream(headers)
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
        final String messageFromDb = jdbcTemplate.queryForObject("select message from otel_demo.storage where trace_id = :traceId",
            Map.of("traceId", traceId), String.class);
        assertThat(messageFromDb)
            .isEqualTo(received.value());
    }

    private long countRecordsInTable() {
        final Long queryResult = jdbcTemplate.getJdbcTemplate().queryForObject("select count(*) from otel_demo.storage", Long.class);
        return Objects.requireNonNullElse(queryResult, 0L);
    }
}
