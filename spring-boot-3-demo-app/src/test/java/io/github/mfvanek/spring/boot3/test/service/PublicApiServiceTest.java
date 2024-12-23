package io.github.mfvanek.spring.boot3.test.service;

import io.github.mfvanek.spring.boot3.test.support.TestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.TimeZone;

import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@EnableWireMock
@ActiveProfiles("test")
public class PublicApiServiceTest extends TestBase {

    @Autowired
    PublicApiService publicApiService;


    @Test
    void printTimeZoneSuccessfully() {
        final String zoneNames = TimeZone.getDefault().getID();

        stubFor(get(urlPathMatching("/" + zoneNames))
            .willReturn(aResponse()
                .withStatus(200)));
        publicApiService.getZonedTime();
    }
}
