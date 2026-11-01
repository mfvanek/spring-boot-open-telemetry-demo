/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.support;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.mfvanek.spring.boot3.test.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot3.test.service.dto.ParsedDateTime;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.wiremock.spring.EnableWireMock;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
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
@ContextConfiguration(
    classes = TestBase.CustomClockConfiguration.class,
    initializers = {KafkaInitializer.class, JaegerInitializer.class, PostgresInitializer.class}
)
@EnableWireMock
public abstract class TestBase {

    private static final ZoneOffset FIXED_ZONE = ZoneOffset.ofHours(-1);
    private static final LocalDateTime BEFORE_MILLENNIUM = LocalDateTime.of(1999, Month.DECEMBER, 31, 23, 59, 59);

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

    protected String stubBadResponse() {
        final String zoneName = TimeZone.getDefault().getID();
        stubBadResponse(zoneName);
        return zoneName;
    }

    private void stubErrorResponse(@NonNull final String zoneName, @NonNull final RuntimeException errorForResponse) {
        stubFor(get(urlPathMatching("/" + zoneName))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody(objectMapper.writeValueAsString(errorForResponse))
            ));
    }

    private void stubBadResponse(String zoneName) {
        stubFor(
            get(urlPathMatching("/" + zoneName))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(objectMapper.writeValueAsString("Bad response"))
                )
        );
    }

    @TestConfiguration
    static class CustomClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(BEFORE_MILLENNIUM.toInstant(FIXED_ZONE), FIXED_ZONE);
        }
    }
}
