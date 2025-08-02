/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.mfvanek.spring.boot3.reactive.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot3.reactive.service.dto.ParsedDateTime;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Clock;
import java.util.TimeZone;
import javax.annotation.Nonnull;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@ActiveProfiles("test")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {KafkaInitializer.class, JaegerInitializer.class, PostgresInitializer.class})
@AutoConfigureWireMock(port = 0)
public abstract class TestBase {

    protected static final String TRACE_ID_HEADER_NAME = "X-Trace-Id";
    @Autowired
    protected WebTestClient webTestClient;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected Clock clock;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @BeforeEach
    void resetExternalMocks() {
        WireMock.resetAllRequests();
    }

    @Nonnull
    protected String stubOkResponse(@Nonnull final ParsedDateTime parsedDateTime) {
        final String zoneName = TimeZone.getDefault().getID();
        stubOkResponse(zoneName, parsedDateTime);
        return zoneName;
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

    @Nonnull
    protected String stubErrorResponse() {
        final String zoneName = TimeZone.getDefault().getID();
        final RuntimeException exception = new RuntimeException("Retries exhausted");
        stubErrorResponse(zoneName, exception);
        return zoneName;
    }

    @SneakyThrows
    private void stubErrorResponse(@Nonnull final String zoneName, @Nonnull final RuntimeException errorForResponse) {
        stubFor(get(urlPathMatching("/" + zoneName))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody(objectMapper.writeValueAsString(errorForResponse))
            ));
    }
}
