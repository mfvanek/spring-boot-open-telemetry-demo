/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.reactive.controllers;

import io.github.mfvanek.spring.boot.reactive.service.dto.ParsedDateTime;
import io.github.mfvanek.spring.boot.reactive.support.TestBase;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Incoming B3 headers must be parsed: the traceId from the request is used in logs and returned in the response.
 * See <a href="https://github.com/openzipkin/b3-propagation">B3 propagation</a>.
 */
@ExtendWith(OutputCaptureExtension.class)
class B3HeadersPropagationTest extends TestBase {

    @BeforeEach
    void stubExternalApi() {
        stubOkResponse(ParsedDateTime.from(LocalDateTime.now(clock).minusDays(1)));
    }

    @Test
    @DisplayName("Single b3 header: {TraceId}-{SpanId}-{SamplingState}")
    void shouldUseTraceIdFromSingleB3Header(@NonNull final CapturedOutput output) {
        final String traceId = "80f198ee56343ba864fe8b2a57d3eff7";

        final String returnedTraceId = callCurrentTime(headers -> headers.add("b3", traceId + "-e457b5a2e4d86bd1-1"));

        assertThat(returnedTraceId)
            .isEqualTo(traceId);
        assertThatTraceIdIsLogged(output, traceId);
    }

    @Test
    @DisplayName("Multiple X-B3-* headers")
    void shouldUseTraceIdFromMultipleB3Headers(@NonNull final CapturedOutput output) {
        final String traceId = "463ac35c9f6413ad48485a3953bb6124";

        final String returnedTraceId = callCurrentTime(headers -> {
            headers.add("X-B3-TraceId", traceId);
            headers.add("X-B3-SpanId", "a2fb4a1d1a96d312");
            headers.add("X-B3-Sampled", "1");
        });

        assertThat(returnedTraceId)
            .isEqualTo(traceId);
        assertThatTraceIdIsLogged(output, traceId);
    }

    @Test
    @DisplayName("64-bit traceId from b3 header is left-padded with zeros to 128 bits")
    void shouldPadShortTraceIdFromB3Header(@NonNull final CapturedOutput output) {
        final String shortTraceId = "64fe8b2a57d3eff7";
        final String expectedTraceId = "0000000000000000" + shortTraceId;

        final String returnedTraceId = callCurrentTime(headers -> headers.add("b3", shortTraceId + "-05e3ac9a4f6e3b90-1"));

        assertThat(returnedTraceId)
            .isEqualTo(expectedTraceId);
        assertThatTraceIdIsLogged(output, expectedTraceId);
    }

    @Nullable
    private String callCurrentTime(@NonNull final Consumer<HttpHeaders> headersConsumer) {
        return webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("current-time")
                .build())
            .headers(headersConsumer)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(LocalDateTime.class)
            .returnResult()
            .getResponseHeaders()
            .getFirst(TRACE_ID_HEADER_NAME);
    }

    private static void assertThatTraceIdIsLogged(@NonNull final CapturedOutput output, @NonNull final String traceId) {
        assertThat(output.getAll())
            .contains("Called method getNow. TraceId = " + traceId)
            .contains("\"traceId\":\"" + traceId + "\"");
    }
}
