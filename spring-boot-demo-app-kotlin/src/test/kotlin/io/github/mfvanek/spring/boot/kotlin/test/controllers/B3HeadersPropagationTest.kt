/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.kotlin.test.controllers

import io.github.mfvanek.spring.boot.kotlin.test.service.dto.toParsedDateTime
import io.github.mfvanek.spring.boot.kotlin.test.support.TestBase
import io.github.mfvanek.spring.boot.traceid.TraceIdResponseHeaders.TRACE_ID_HEADER_NAME
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.http.HttpHeaders
import java.time.LocalDateTime

/**
 * Incoming B3 headers must be parsed: the traceId from the request is used in logs and returned in the response.
 * See [B3 propagation](https://github.com/openzipkin/b3-propagation).
 */
@ExtendWith(OutputCaptureExtension::class)
class B3HeadersPropagationTest : TestBase() {
    @BeforeEach
    fun stubExternalApi() {
        stubOkResponse(LocalDateTime.now(clock).minusDays(1).toParsedDateTime())
    }

    @Test
    @DisplayName("Single b3 header: {TraceId}-{SpanId}-{SamplingState}")
    fun shouldUseTraceIdFromSingleB3Header(output: CapturedOutput) {
        val traceId = "80f198ee56343ba864fe8b2a57d3eff7"

        val returnedTraceId = callCurrentTime { it.add("b3", "$traceId-e457b5a2e4d86bd1-1") }

        assertThat(returnedTraceId)
            .isEqualTo(traceId)
        assertThatTraceIdIsLogged(output, traceId)
    }

    @Test
    @DisplayName("Multiple X-B3-* headers")
    fun shouldUseTraceIdFromMultipleB3Headers(output: CapturedOutput) {
        val traceId = "463ac35c9f6413ad48485a3953bb6124"

        val returnedTraceId = callCurrentTime {
            it.add("X-B3-TraceId", traceId)
            it.add("X-B3-SpanId", "a2fb4a1d1a96d312")
            it.add("X-B3-Sampled", "1")
        }

        assertThat(returnedTraceId)
            .isEqualTo(traceId)
        assertThatTraceIdIsLogged(output, traceId)
    }

    @Test
    @DisplayName("64-bit traceId from b3 header is left-padded with zeros to 128 bits")
    fun shouldPadShortTraceIdFromB3Header(output: CapturedOutput) {
        val shortTraceId = "64fe8b2a57d3eff7"
        val expectedTraceId = "0000000000000000$shortTraceId"

        val returnedTraceId = callCurrentTime { it.add("b3", "$shortTraceId-05e3ac9a4f6e3b90-1") }

        assertThat(returnedTraceId)
            .isEqualTo(expectedTraceId)
        assertThatTraceIdIsLogged(output, expectedTraceId)
    }

    private fun callCurrentTime(headersConsumer: (HttpHeaders) -> Unit): String? =
        webTestClient.get()
            .uri { it.path("current-time").build() }
            .headers(headersConsumer)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(LocalDateTime::class.java)
            .returnResult()
            .responseHeaders
            .getFirst(TRACE_ID_HEADER_NAME)

    private fun assertThatTraceIdIsLogged(output: CapturedOutput, traceId: String) {
        assertThat(output.all)
            .contains("Called method getNow. TraceId = $traceId")
            .contains("\"traceId\":\"$traceId\"")
    }
}
