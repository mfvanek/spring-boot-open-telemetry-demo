package io.github.mfvanek.spring.boot3.test.service;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mfvanek.spring.boot3.test.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot3.test.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot3.test.support.TestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@WireMockTest(httpPort = 8081)
@ActiveProfiles("test")
public class PublicApiServiceTest extends TestBase {

    @Autowired
    PublicApiService publicApiService;

    @Test
    void printTimeZoneSuccessfully() {
        final String zoneNames = TimeZone.getDefault().getID();
        final ObjectMapper mapper = new ObjectMapper();
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
            }
        );
    }
    @Test
    void retriesThreeTimesToGetZonedTime() {
        final String zoneNames = TimeZone.getDefault().getID();
        final ObjectMapper mapper = new ObjectMapper();
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
            () -> assertThat(parsingExceptionResult).isNull()
        );
    }
}
