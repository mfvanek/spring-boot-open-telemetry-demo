package io.github.mfvanek.spring.boot2.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mfvanek.spring.boot2.test.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot2.test.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot2.test.support.TestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import javax.annotation.Nonnull;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(OutputCaptureExtension.class)
@WireMockTest(httpPort = 9999)
public class PublicApiServiceTest extends TestBase {

    @Autowired
    PublicApiService publicApiService;

    @Autowired
    ObjectMapper mapper;

    @Test
    void getZonedTimeSuccessfully(@Nonnull final CapturedOutput output) {
        final String zoneNames = TimeZone.getDefault().getID();
        final LocalDateTime localDateTimeNow = LocalDateTime.now(ZoneId.systemDefault());
        final ParsedDateTime parsedDateTime = new ParsedDateTime(
            localDateTimeNow.getYear(),
            localDateTimeNow.getMonthValue(),
            localDateTimeNow.getDayOfMonth(),
            localDateTimeNow.getHour(),
            localDateTimeNow.getMinute());
        final CurrentTime currentTime = new CurrentTime(parsedDateTime);
        LocalDateTime answer;
        try {
            stubFor(get(urlPathMatching("/" + zoneNames))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(mapper.writeValueAsString(currentTime))
                ));
            answer = publicApiService.getZonedTime();
        } catch (JsonProcessingException e) {
            answer = null;
        }
        final LocalDateTime result = answer;
        verify(getRequestedFor(urlPathMatching("/" + zoneNames)));
        assertAll(
            () -> {
                assertThat(result).isNotNull();
                assertThat(result.truncatedTo(ChronoUnit.MINUTES)).isEqualTo(localDateTimeNow.truncatedTo(ChronoUnit.MINUTES));
            },
            () -> assertThat(output).doesNotContain(
                "Retrying request to ",
                "Retries exhausted",
                "Failed to convert response ")
        );
    }

    @Test
    void retriesThreeTimesToGetZonedTime(@Nonnull final CapturedOutput output) {
        final String zoneNames = TimeZone.getDefault().getID();
        LocalDateTime answer;
        JsonProcessingException jsonProcessingException = null;
        final RuntimeException exception = new RuntimeException("Retries exhausted");
        try {
            stubFor(get(urlPathMatching("/" + zoneNames))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody(mapper.writeValueAsString(exception))
                ));
            answer = publicApiService.getZonedTime();
        } catch (JsonProcessingException e) {
            jsonProcessingException = e;
            answer = null;
        }
        final LocalDateTime result = answer;
        final JsonProcessingException parsingExceptionResult = jsonProcessingException;
        verify(1 + 3, getRequestedFor(urlPathMatching("/" + zoneNames)));
        assertAll(
            () -> assertThat(result).isNull(),
            () -> assertThat(parsingExceptionResult).isNull(),
            () -> assertThat(output).contains(
                "Retrying request to ",
                "Retries exhausted"
            ),
            () -> assertThat(output).doesNotContain("Failed to convert response ")
        );
    }
}
