/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot2.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.mfvanek.spring.boot2.test.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot2.test.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot2.test.support.TestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import javax.annotation.Nonnull;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class PublicApiServiceTest extends TestBase {

    @Autowired
    private PublicApiService publicApiService;

    @Test
    void getZonedTimeSuccessfully(@Nonnull final CapturedOutput output) throws JsonProcessingException {
        final String zoneNames = TimeZone.getDefault().getID();
        final LocalDateTime localDateTimeNow = LocalDateTime.now(ZoneId.systemDefault());
        final ParsedDateTime parsedDateTime = ParsedDateTime.from(localDateTimeNow);
        final CurrentTime currentTime = new CurrentTime(parsedDateTime);
        stubFor(get(urlPathMatching("/" + zoneNames))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(objectMapper.writeValueAsString(currentTime))
            ));

        final LocalDateTime result = publicApiService.getZonedTime();
        verify(getRequestedFor(urlPathMatching("/" + zoneNames)));

        assertThat(result).isNotNull();
        assertThat(result.truncatedTo(ChronoUnit.MINUTES))
            .isEqualTo(localDateTimeNow.truncatedTo(ChronoUnit.MINUTES));
        assertThat(output.getAll()).doesNotContain(
            "Retrying request to ",
            "Retries exhausted",
            "Failed to convert response ",
            "timezone");
    }

    @Test
    void retriesOnceToGetZonedTime(@Nonnull final CapturedOutput output) throws JsonProcessingException {
        resetAllRequests();
        final String zoneNames = TimeZone.getDefault().getID();
        final RuntimeException exception = new RuntimeException("Retries exhausted");
        stubFor(get(urlPathMatching("/" + zoneNames))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody(objectMapper.writeValueAsString(exception))
            ));

        final LocalDateTime result = publicApiService.getZonedTime();
        verify(2, getRequestedFor(urlPathMatching("/" + zoneNames)));

        assertThat(result).isNull();
        assertThat(output.getAll()).contains("Retrying request to ", "Retries exhausted", "\"instance_timezone\":\"" + zoneNames + "\"");
        assertThat(output.getAll()).doesNotContain("Failed to convert response ");
    }
}
