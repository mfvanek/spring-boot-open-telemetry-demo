/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.reactive.support;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.mfvanek.spring.boot.initializers.JaegerInitializer;
import io.github.mfvanek.spring.boot.initializers.KafkaInitializer;
import io.github.mfvanek.spring.boot.initializers.PostgresInitializer;
import io.github.mfvanek.spring.boot.reactive.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot.reactive.service.dto.ParsedDateTime;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.wiremock.spring.EnableWireMock;
import reactor.blockhound.BlockHound;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.TimeZone;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@ActiveProfiles("test")
@AutoConfigureTracing
@AutoConfigureMetrics
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {KafkaInitializer.class, JaegerInitializer.class, PostgresInitializer.class})
@EnableWireMock
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

    @BeforeAll
    static void initBlockHound() {
        BlockHound.install();
    }

    @BeforeEach
    void resetExternalMocks() {
        WireMock.resetAllRequests();
    }

    @NonNull
    protected String stubOkResponse(@NonNull final ParsedDateTime parsedDateTime) {
        final String zoneName = TimeZone.getDefault().getID();
        stubOkResponse(zoneName, parsedDateTime);
        return zoneName;
    }

    private void stubOkResponse(@NonNull final String zoneName, @NonNull final ParsedDateTime parsedDateTime) {
        final CurrentTime currentTime = new CurrentTime(parsedDateTime);
        stubFor(get(urlPathMatching("/" + zoneName))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString(currentTime))
            ));
    }

    @NonNull
    protected String stubErrorResponse() {
        final String zoneName = TimeZone.getDefault().getID();
        final RuntimeException exception = new RuntimeException("Retries exhausted");
        stubErrorResponse(zoneName, exception);
        return zoneName;
    }

    private void stubErrorResponse(@NonNull final String zoneName, @NonNull final RuntimeException errorForResponse) {
        stubFor(get(urlPathMatching("/" + zoneName))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody(objectMapper.writeValueAsString(errorForResponse))
            ));
    }

    @NonNull
    protected String stubOkButNotCorrectResponse() {
        final String zoneName = TimeZone.getDefault().getID();
        stubOkButNotCorrectResponse(zoneName);
        return zoneName;
    }

    private void stubOkButNotCorrectResponse(@NonNull final String zoneName) {
        stubFor(get(urlPathMatching("/" + zoneName))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString("bad response"))
            ));
    }
}
