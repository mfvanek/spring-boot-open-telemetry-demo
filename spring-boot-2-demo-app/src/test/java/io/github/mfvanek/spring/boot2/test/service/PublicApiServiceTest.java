package io.github.mfvanek.spring.boot2.test.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mfvanek.spring.boot2.test.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot2.test.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot2.test.support.TestBase;

import java.time.LocalDateTime;

import java.time.ZoneId;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import java.util.TimeZone;

import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWireMock
@ActiveProfiles("test")
public class PublicApiServiceTest extends TestBase {

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    PublicApiService publicApiService;

    @Test
    void printTimeZoneSuccessfully() throws JsonProcessingException {
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
        wireMockServer.stubFor(get(urlPathMatching("/" + zoneNames))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(mapper.writeValueAsString(currentTime))
            ));
        var result = publicApiService.getZonedTime();

        wireMockServer.verify(getRequestedFor(urlPathMatching("/" + zoneNames)));
        assertThat(result.truncatedTo(ChronoUnit.MINUTES)).isEqualTo(localDateTimeNow.truncatedTo(ChronoUnit.MINUTES));
    }

}
