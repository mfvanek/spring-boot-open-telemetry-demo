package io.github.mfvanek.spring.boot2.test.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mfvanek.spring.boot2.test.config.WebClientConfig;
import io.github.mfvanek.spring.boot2.test.support.KafkaInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.TimeZone;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(classes = {PublicApiService.class, WebClientConfig.class, KafkaInitializer.class})
@WireMockTest
@ActiveProfiles("test")
public class PublicApiServiceTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
        .options(wireMockConfig().port(8080))
        .build();
    @Autowired
    PublicApiService publicApiService;

    @Test
    void printTimeZoneSuccessfully() {
        final String zoneNames = TimeZone.getDefault().getID();
        wm.stubFor(get(urlPathMatching("/" + zoneNames))
            .willReturn(aResponse()
                .withStatus(200)));
        publicApiService.getZonedTime();

        wm.verify(getRequestedFor(urlPathMatching("/" + zoneNames)));
    }

}
