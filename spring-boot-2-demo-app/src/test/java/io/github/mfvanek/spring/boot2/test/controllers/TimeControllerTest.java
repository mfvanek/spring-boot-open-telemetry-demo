package io.github.mfvanek.spring.boot2.test.controllers;

import io.github.mfvanek.spring.boot2.test.support.KafkaConsumerUtils;
import io.github.mfvanek.spring.boot2.test.support.TestBase;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static io.github.mfvanek.spring.boot2.test.filters.TraceIdInResponseServletFilter.TRACE_ID_HEADER_NAME;
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
    }
}
