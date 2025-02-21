/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.service;

import io.github.mfvanek.spring.boot3.test.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot3.test.support.TestBase;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nonnull;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test-retry")
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

        final LocalDateTime result = publicApiService.getZonedTime();
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
    void retriesOnceToGetZonedTime(@Nonnull final CapturedOutput output) {
        final String zoneName = stubErrorResponse();

        Observation.createNotStarted("test", observationRegistry).observe(() -> {
            final String traceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();

            final LocalDateTime result = publicApiService.getZonedTime();
            assertThat(result).isNull();

            assertThat(output.getAll())
                .containsPattern(String.format(Locale.ROOT,
                    ".*\\[%s-[a-fA-F0-9]{16}] i\\.g\\.m\\.s\\.b\\.test\\.service\\.PublicApiService {2}: Retrying request to",
                    traceId))
                .containsPattern(String.format(Locale.ROOT,
                    ".*\\[%s-[a-fA-F0-9]{16}] i\\.g\\.m\\.s\\.b\\.test\\.service\\.PublicApiService {2}: Request to '/%s' failed after 2 attempts",
                    traceId, zoneName))
                .doesNotContain("Failed to convert response ");
        });

        verify(2, getRequestedFor(urlPathMatching("/" + zoneName)));
    }
}
