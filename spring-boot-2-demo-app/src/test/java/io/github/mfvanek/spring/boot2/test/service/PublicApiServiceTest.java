/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot2.test.service;

import io.github.mfvanek.spring.boot2.test.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot2.test.support.TestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.sleuth.ScopedSpan;
import org.springframework.cloud.sleuth.Tracer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
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

    @Test
    void getZonedTimeSuccessfully(@Nonnull final CapturedOutput output) {
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
        final ScopedSpan span = tracer.startScopedSpan("test");
        try {
            final LocalDateTime result = publicApiService.getZonedTime();
            assertThat(result).isNull();

            final String traceId = span.context().traceId();
            assertThat(output.getAll())
                .containsPattern(String.format(Locale.ROOT,
                    ".*\"message\":\"Retrying request to '/%s', attempt 1/1 due to error:\"," +
                        "\"logger\":\"io\\.github\\.mfvanek\\.spring\\.boot2\\.test\\.service\\.PublicApiService\"," +
                        "\"thread\":\"[^\"]+\",\"level\":\"INFO\",\"stack_trace\":\".+?\"," +
                        "\"traceId\":\"%s\",\"spanId\":\"[a-f0-9]+\",\"instance_timezone\":\"%s\",\"applicationName\":\"spring-boot-2-demo-app\"}%n",
                    zoneName, traceId, zoneName))
                .containsPattern(String.format(Locale.ROOT,
                    ".*\"message\":\"Request to '/%s' failed after 2 attempts.\"," +
                        "\"logger\":\"io\\.github\\.mfvanek\\.spring\\.boot2\\.test\\.service\\.PublicApiService\"," +
                        "\"thread\":\"[^\"]+\",\"level\":\"ERROR\",\"traceId\":\"%s\",\"spanId\":\"[a-f0-9]+\",\"applicationName\":\"spring-boot-2-demo-app\"}%n",
                    zoneName, traceId))
                .doesNotContain("Failed to convert response ");
        } finally {
            span.end();
        }

        verify(2, getRequestedFor(urlPathMatching("/" + zoneName)));
    }
}
