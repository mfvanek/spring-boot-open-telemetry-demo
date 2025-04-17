package io.github.mfvanek.spring.boot3.kotlin.test

import io.github.mfvanek.spring.boot3.kotlin.test.support.TestBase
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.test.web.server.LocalManagementPort
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriBuilder

class ActuatorEndpointTest : TestBase() {
    @LocalServerPort
    private val port = 0

    @LocalManagementPort
    private val actuatorPort = 0

    private lateinit var actuatorClient: WebTestClient

    @BeforeEach
    fun setUp() {
        this.actuatorClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$actuatorPort/actuator/")
            .build()
    }

    @Test
    fun actuatorShouldBeRunOnSeparatePort() {
        Assertions.assertThat(actuatorPort)
            .isNotEqualTo(port)
    }

    @ParameterizedTest
    @CsvSource(
        value = ["prometheus|jvm_threads_live_threads|text/plain", "health|{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}|application/json", "health/liveness|{\"status\":\"UP\"}|application/json", "health/readiness|{\"status\":\"UP\"}|application/json", "info|\"version\":|application/json"],
        delimiter = '|'
    )
    fun actuatorEndpointShouldReturnOk(
        endpointName: String,
        expectedSubstring: String,
        mediaType: String
    ) {
        val result = actuatorClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .path(endpointName)
                    .build()
            }
            .accept(MediaType.valueOf(mediaType))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
        assertThat(result)
            .contains(expectedSubstring)
    }

    @Test
    fun swaggerUiEndpointShouldReturnFound() {
        val result = actuatorClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .pathSegment("swagger-ui")
                    .build()
            }
            .accept(MediaType.TEXT_HTML)
            .exchange()
            .expectStatus().isFound()
            .expectHeader().location("/actuator/swagger-ui/index.html")
            .expectBody()
            .returnResult()
            .responseBody
        assertThat(result).isNull()
    }

    @Test
    fun readinessProbeShouldBeCollectedFromApplicationMainPort() {
        val result = webTestClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .pathSegment("readyz")
                    .build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
        assertThat(result)
            .isEqualTo("{\"status\":\"UP\"}")

        val metricsResult = actuatorClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder
                    .path("prometheus")
                    .build()
            }
            .accept(MediaType.valueOf("text/plain"))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
        assertThat(metricsResult)
            .contains("http_server_requests_seconds_bucket")
    }
}
