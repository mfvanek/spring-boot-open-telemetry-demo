/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.service;

import io.github.mfvanek.spring.boot3.reactive.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot3.reactive.support.TestBase;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nonnull;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class PublicApiServiceTest extends TestBase {

    @Autowired
    private PublicApiService publicApiService;
    @Autowired
    private Tracer tracer;
    @Autowired
    private ObservationRegistry observationRegistry;

    @Test
    void printTimeZoneSuccessfully(@Nonnull final CapturedOutput output) {
        final LocalDateTime localDateTimeNow = LocalDateTime.now(clock);
        final String zoneName = stubOkResponse(ParsedDateTime.from(localDateTimeNow));

        final LocalDateTime result = Objects.requireNonNull(publicApiService.getZonedTime()).block();
        verify(getRequestedFor(urlPathMatching("/" + zoneName)));
        assertThat(result).isNotNull();
        assertThat(result.truncatedTo(ChronoUnit.MINUTES))
            .isEqualTo(localDateTimeNow.truncatedTo(ChronoUnit.MINUTES));
        assertThat(output.getAll())
            .contains("Request received:")
            .doesNotContain(
                "Retrying request to ",
                "Retries exhausted",
                "Failed to convert response ",
                "timezone");
    }

    @Test
    void printTimeZoneSuccessfullyWithStepVerifier() {
        final LocalDateTime localDateTimeNow = LocalDateTime.now(clock);
        stubOkResponse(ParsedDateTime.from(localDateTimeNow));

        StepVerifier.create(publicApiService.getZonedTime())
            .expectNext(localDateTimeNow.truncatedTo(ChronoUnit.MINUTES))
            .verifyComplete();
    }

    @Test
    void retriesOnceToGetZonedTime(@Nonnull final CapturedOutput output) {
        final String zoneName = stubErrorResponse();

        Observation.createNotStarted("test", observationRegistry).observe(() -> {
            final String traceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();

            final LocalDateTime result = publicApiService.getZonedTime().block();
            assertThat(result).isNull();

            assertThat(output.getAll())
                .containsPattern(String.format(Locale.ROOT,
                    ".*\"message\":\"Retrying request to '[^']+?', attempt 1/1 due to error:\"," +
                        "\"logger\":\"io\\.github\\.mfvanek\\.spring\\.boot3\\.reactive\\.service\\.PublicApiService\"," +
                        "\"thread\":\"[^\"]+\",\"level\":\"INFO\",\"stack_trace\":\".+?\"," +
                        "\"traceId\":\"%s\",\"spanId\":\"[a-f0-9]+\",\"instance_timezone\":\"%s\",\"applicationName\":\"spring-boot-3-demo-app-reactive\"\\}%n", traceId, zoneName))
                .containsPattern(String.format(Locale.ROOT,
                    ".*\"message\":\"Request to '[^']+?' failed after 2 attempts.\"," +
                        "\"logger\":\"io\\.github\\.mfvanek\\.spring\\.boot3\\.reactive\\.service\\.PublicApiService\"," +
                        "\"thread\":\"[^\"]+\",\"level\":\"ERROR\",\"traceId\":\"%s\",\"spanId\":\"[a-f0-9]+\",\"applicationName\":\"spring-boot-3-demo-app-reactive\"}%n", traceId))
                .doesNotContain("Failed to convert response ");
        });

        verify(2, getRequestedFor(urlPathMatching("/" + zoneName)));
    }

    @Test
    void emptyResponseWhen500StatusWithStepVerifier() {
        stubErrorResponse();

        StepVerifier.create(publicApiService.getZonedTime())
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    void emptyResponseWhen200StatusWithBadResposeWithStepVerifier(@Nonnull final CapturedOutput output) {
        stubOkButNotCorrectResponse();

        StepVerifier.create(publicApiService.getZonedTime())
            .expectNextCount(0)
            .verifyComplete();
    }
}
