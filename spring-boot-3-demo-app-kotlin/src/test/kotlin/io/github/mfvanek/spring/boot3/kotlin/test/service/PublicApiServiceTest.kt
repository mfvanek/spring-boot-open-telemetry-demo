package io.github.mfvanek.spring.boot3.kotlin.test.service

import com.github.tomakehurst.wiremock.client.WireMock
import io.github.mfvanek.spring.boot3.kotlin.test.service.dto.toParsedDateTime
import io.github.mfvanek.spring.boot3.kotlin.test.support.TestBase
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.tracing.Tracer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

@ExtendWith(OutputCaptureExtension::class)
class PublicApiServiceTest : TestBase() {
    @Autowired
    private lateinit var publicApiService: PublicApiService

    @Autowired
    private lateinit var tracer: Tracer

    @Autowired
    private lateinit var observationRegistry: ObservationRegistry

    @Test
    fun printTimeZoneSuccessfully(output: CapturedOutput) {
        val localDateTimeNow = LocalDateTime.now(clock)
        val zoneName =
            stubOkResponse(localDateTimeNow.toParsedDateTime())

        val result = publicApiService.getZonedTime()
        WireMock.verify(
            WireMock.getRequestedFor(
                WireMock.urlPathMatching(
                    "/$zoneName"
                )
            )
        )

        assertThat(result).isNotNull()
        assertThat(result!!.truncatedTo(ChronoUnit.MINUTES))
            .isEqualTo(localDateTimeNow.truncatedTo(ChronoUnit.MINUTES))
        assertThat(output.all)
            .contains("Request received:")
            .doesNotContain(
                "Retrying request to ",
                "Retries exhausted",
                "Failed to convert response ",
                "timezone"
            )
    }

    @Test
    fun retriesOnceToGetZonedTime(output: CapturedOutput) {
        val zoneName = stubErrorResponse()

        Observation.createNotStarted("test", observationRegistry).observe {
            val traceId = tracer.currentSpan()!!.context().traceId()
            val result = publicApiService.getZonedTime()
            assertThat(result).isNull()
            assertThat(output.all)
                .containsPattern(
                    String.format(
                        Locale.ROOT,
                        ".*\"message\":\"Retrying request to '[^']+?', attempt 1/1 due to error: .+?," +
                            "\"logger\":\"io\\.github\\.mfvanek\\.spring\\.boot3\\.kotlin\\.test\\.service\\.PublicApiService\"," +
                            "\"thread\":\"[^\"]+\",\"level\":\"INFO\"," +
                            "\"traceId\":\"%s\",\"spanId\":\"[a-f0-9]+\",\"instance_timezone\":\"%s\",\"applicationName\":\"spring-boot-3-demo-app\"\\}%n",
                        traceId,
                        zoneName
                    )
                        .toPattern()
                )
                .containsPattern(
                    String.format(
                        Locale.ROOT,
                        ".*\"message\":\"Request to '[^']+?' failed after 2 attempts.\"," +
                            "\"logger\":\"io\\.github\\.mfvanek\\.spring\\.boot3\\.kotlin\\.test\\.service\\.PublicApiService\"," +
                            "\"thread\":\"[^\"]+\",\"level\":\"ERROR\",\"traceId\":\"%s\",\"spanId\":\"[a-f0-9]+\",\"applicationName\":\"spring-boot-3-demo-app\"}%n",
                        traceId
                    )
                        .toPattern()
                )
                .doesNotContain("Failed to convert response ")
        }
        WireMock.verify(
            2,
            WireMock.getRequestedFor(
                WireMock.urlPathMatching(
                    "/$zoneName"
                )
            )
        )
    }

    @Test
    fun throwsJsonProcessingExceptionWithBadResponse(output: CapturedOutput) {
        stubBadResponse()
        Observation.createNotStarted("test", observationRegistry).observe {
            val result = publicApiService.getZonedTime()
            assertThat(result).isNull()
            assertThat(tracer.currentSpan()?.context()?.traceId()).isNotNull()
            assertThat(output.all).contains("Failed to convert response")
        }
    }
}
