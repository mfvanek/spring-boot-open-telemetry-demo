/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfvanek.db.migrations.common.saver.DbSaver;
import io.github.mfvanek.spring.boot3.test.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot3.test.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot3.test.support.JaegerInitializer;
import io.github.mfvanek.spring.boot3.test.support.KafkaConsumerUtils;
import io.github.mfvanek.spring.boot3.test.support.KafkaInitializer;
import io.github.mfvanek.spring.boot3.test.support.PostgresInitializer;
import io.github.mfvanek.spring.boot3.test.support.SpanExporterConfiguration;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.annotation.Nonnull;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.github.mfvanek.spring.boot3.test.filters.TraceIdInResponseServletFilter.TRACE_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SuppressWarnings("checkstyle:classfanoutcomplexity")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(
    classes = SpanExporterConfiguration.class,
    initializers = {KafkaInitializer.class, JaegerInitializer.class, PostgresInitializer.class}
)
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaTracingTest {

    @Autowired
    protected WebTestClient webTestClient;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected Clock clock;
    @Autowired
    private InMemorySpanExporter spanExporter;
    @Autowired
    private KafkaProperties kafkaProperties;
    @MockitoBean
    private DbSaver dbSaver;
    private KafkaMessageListenerContainer<UUID, String> container;
    private final BlockingQueue<ConsumerRecord<UUID, String>> consumerRecords = new ArrayBlockingQueue<>(4);

    @BeforeAll
    void setUpKafkaConsumerAndResetTelemetry() {
        GlobalOpenTelemetry.resetForTest();
        container = KafkaConsumerUtils.setUpKafkaConsumer(kafkaProperties, consumerRecords);
    }

    @AfterAll
    void tearDownKafkaConsumer() {
        if (container != null) {
            container.stop();
            container = null;
        }
    }

    @Test
    void closeAllSpansWhenException() {
        final Exception testException = new RuntimeException("saving failed");
        doThrow(testException).when(dbSaver).processSingleRecord(any());
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
        final List<SpanData> finishedSpans = spanExporter.getFinishedSpanItems();

        assertThat(finishedSpans.stream().map(SpanData::getTraceId)).contains(traceId);
        assertThat(finishedSpans.stream().map(SpanData::getStatus)).contains(StatusData.create(StatusCode.ERROR, "saving failed"));
        assertThat(finishedSpans.stream().map(SpanData::getName)).contains("processing-record-from-kafka");
    }

    protected void stubOkResponse(@Nonnull final ParsedDateTime parsedDateTime) {
        final String zoneName = TimeZone.getDefault().getID();
        stubOkResponse(zoneName, parsedDateTime);
    }

    @SneakyThrows
    private void stubOkResponse(@Nonnull final String zoneName, @Nonnull final ParsedDateTime parsedDateTime) {
        final CurrentTime currentTime = new CurrentTime(parsedDateTime);
        stubFor(get(urlPathMatching("/" + zoneName))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString(currentTime))
            ));
    }
}

