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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nonnull;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class PublicApiServiceTest extends TestBase {

    @Autowired
    private PublicApiService publicApiService;

    @Test
    void getZonedTimeSuccessfully(@Nonnull final CapturedOutput output) {
        final LocalDateTime localDateTimeNow = LocalDateTime.now(clock);
        final String zoneNames = stubOkResponse(ParsedDateTime.from(localDateTimeNow));

        final LocalDateTime result = publicApiService.getZonedTime();
        verify(getRequestedFor(urlPathMatching("/" + zoneNames)));

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
        final String zoneNames = stubErrorResponse();

        final LocalDateTime result = publicApiService.getZonedTime();
        verify(2, getRequestedFor(urlPathMatching("/" + zoneNames)));

        assertThat(result).isNull();
        assertThat(output.getAll())
            .contains("Retrying request to ", "Retries exhausted", "\"instance_timezone\":\"" + zoneNames + "\"")
            .doesNotContain("Failed to convert response ");
    }
}
