/*
* Copyright (c) 2020-2025. Ivan Vakhrushev and others.
* https://github.com/mfvanek/spring-boot-open-telemetry-demo
*
* Licensed under the Apache License 2.0
*/

package io.github.mfvanek.spring.boot3.kotlin.test.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import io.github.mfvanek.db.migrations.common.saver.DbSaver
import io.github.mfvanek.spring.boot3.kotlin.test.filters.TraceIdInResponseServletFilter.Companion.TRACE_ID_HEADER_NAME
import io.github.mfvanek.spring.boot3.kotlin.test.service.dto.CurrentTime
import io.github.mfvanek.spring.boot3.kotlin.test.service.dto.ParsedDateTime
import io.github.mfvanek.spring.boot3.kotlin.test.service.dto.toParsedDateTime
import io.github.mfvanek.spring.boot3.kotlin.test.support.JaegerInitializer
import io.github.mfvanek.spring.boot3.kotlin.test.support.KafkaInitializer
import io.github.mfvanek.spring.boot3.kotlin.test.support.PostgresInitializer
import io.github.mfvanek.spring.boot3.kotlin.test.support.SpanExporterConfiguration
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.StatusData
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriBuilder
import java.time.Clock
import java.time.LocalDateTime
import java.util.*
import java.util.function.Function

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(
    classes = [SpanExporterConfiguration::class],
    initializers = [KafkaInitializer::class, JaegerInitializer::class, PostgresInitializer::class]
)
@AutoConfigureWireMock(port = 0)
internal class KafkaTracingTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var clock: Clock

    @Autowired
    private lateinit var spanExporter: InMemorySpanExporter

    @MockitoBean
    private lateinit var dbSaver: DbSaver

    companion object {
        @JvmStatic
        @BeforeAll
        fun resetTelemetry() {
            GlobalOpenTelemetry.resetForTest()
        }
    }

    @Test
    fun closeAllSpansWhenException() {
        val testException: Exception = RuntimeException("saving failed")
        Mockito.doThrow(
            testException
        ).`when`(dbSaver).processSingleRecord(ArgumentMatchers.any<ConsumerRecord<UUID, String>>())
        stubOkResponse((LocalDateTime.now(clock).minusDays(1)).toParsedDateTime())

        val result = webTestClient.get()
            .uri(
                Function { uriBuilder: UriBuilder? ->
                    uriBuilder!!.path("current-time")
                        .build()
                }
            )
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody<LocalDateTime?>(LocalDateTime::class.java)
            .returnResult()
        val traceId = result.responseHeaders.getFirst(TRACE_ID_HEADER_NAME)
        val finishedSpans = spanExporter.finishedSpanItems

        assertThat(finishedSpans.map { it.traceId }).contains(traceId)
        assertThat(finishedSpans.map { it.status }).contains(StatusData.create(StatusCode.ERROR, "saving failed"))
        assertThat(finishedSpans.map { it.name }).contains("processing-record-from-kafka")
    }

    private fun stubOkResponse(parsedDateTime: ParsedDateTime): String {
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
}
