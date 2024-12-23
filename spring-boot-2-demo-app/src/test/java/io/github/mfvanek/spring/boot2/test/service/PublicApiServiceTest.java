package io.github.mfvanek.spring.boot2.test.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mfvanek.spring.boot2.test.support.TestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import java.util.TimeZone;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@AutoConfigureWireMock(port = 8080)
@ActiveProfiles("test")
public class PublicApiServiceTest extends TestBase {

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    PublicApiService publicApiService;

    @Test
    void printTimeZoneSuccessfully() {
        final String zoneNames = TimeZone.getDefault().getID();
        wireMockServer.stubFor(get(urlPathMatching("/" + zoneNames))
            .willReturn(aResponse()
                .withStatus(200)));
        publicApiService.getZonedTime();

        wireMockServer.verify(getRequestedFor(urlPathMatching("/" + zoneNames)));
    }

}
