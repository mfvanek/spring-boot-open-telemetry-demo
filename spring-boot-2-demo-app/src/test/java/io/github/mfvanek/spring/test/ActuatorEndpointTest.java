package io.github.mfvanek.spring.test;

import io.github.mfvanek.spring.test.support.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorEndpointTest extends TestBase {

    @LocalServerPort
    private int port;
    @LocalManagementPort
    private int actuatorPort;

    private WebTestClient actuatorClient;

    @BeforeEach
    void setUp() {
        this.actuatorClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + actuatorPort + "/actuator/")
                .build();
    }

    @Test
    void actuatorShouldBeRunOnSeparatePort() {
        assertThat(actuatorPort)
                .isNotEqualTo(port);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "prometheus|jvm_threads_live_threads|text/plain",
            "health|{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}|application/json",
            "health/liveness|{\"status\":\"UP\"}|application/json",
            "health/readiness|{\"status\":\"UP\"}|application/json",
            "info|\"version\":|application/json"}, delimiter = '|')
    void actuatorEndpointShouldReturnOk(@Nonnull final String endpointName,
                                        @Nonnull final String expectedSubstring,
                                        @Nonnull final String mediaType) {
        final var result = actuatorClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(endpointName)
                        .build())
                .accept(MediaType.valueOf(mediaType))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(result)
                .contains(expectedSubstring);
    }

    @Test
    void swaggerUiEndpointShouldReturnFound() {
        final var result = actuatorClient.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("swagger-ui")
                        .build())
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isFound()
                .expectHeader().location("/actuator/swagger-ui/index.html")
                .expectBody()
                .returnResult()
                .getResponseBody();
        assertThat(result).isNull();
    }

    @Test
    void readinessProbeShouldBeCollectedFromApplicationMainPort() {
        final var result = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("readyz")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(result)
                .isEqualTo("{\"status\":\"UP\"}");
    }
}
