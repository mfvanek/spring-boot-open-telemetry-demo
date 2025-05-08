package io.github.mfvanek.spring.boot3.kotlin.test.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import io.github.mfvanek.spring.boot3.kotlin.test.service.dto.CurrentTime
import io.github.mfvanek.spring.boot3.kotlin.test.service.dto.ParsedDateTime
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Clock
import java.util.*

@ActiveProfiles("test")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [KafkaInitializer::class, JaegerInitializer::class, PostgresInitializer::class])
@AutoConfigureWireMock(port = 0)
abstract class TestBase {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var clock: Clock

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @BeforeEach
    fun resetExternalMocks() {
        WireMock.resetAllRequests()
    }

    protected fun stubOkResponse(parsedDateTime: ParsedDateTime): String {
        val zoneName = TimeZone.getDefault().id
        stubOkResponse(zoneName, parsedDateTime)
        return zoneName
    }

    private fun stubOkResponse(zoneName: String, parsedDateTime: ParsedDateTime) {
        val currentTime = CurrentTime(parsedDateTime)
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching("/$zoneName"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(objectMapper.writeValueAsString(currentTime))
                )
        )
    }

    protected fun stubErrorResponse(): String {
        val zoneName = TimeZone.getDefault().id
        val exception = RuntimeException("Retries exhausted")
        stubErrorResponse(zoneName, exception)
        return zoneName
    }

    protected fun stubBadResponse(): String {
        val zoneName = TimeZone.getDefault().id
        stubBadResponse(zoneName)
        return zoneName
    }

    private fun stubErrorResponse(zoneName: String, errorForResponse: RuntimeException) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching("/$zoneName"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                        .withBody(objectMapper.writeValueAsString(errorForResponse))
                )
        )
    }
    private fun stubBadResponse(zoneName: String) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching("/$zoneName"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(objectMapper.writeValueAsString("Bad response"))
                )
        )
    }
}
