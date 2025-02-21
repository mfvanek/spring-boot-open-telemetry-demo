/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot2.test.controllers;

import io.github.mfvanek.spring.boot2.test.support.RetryTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import java.time.LocalDateTime;
import java.util.Locale;
import javax.annotation.Nonnull;

import static io.github.mfvanek.spring.boot2.test.filters.TraceIdInResponseServletFilter.TRACE_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class TimeControllerRetryTest extends RetryTestBase {

    @Test
    void spanAndMdcShouldBeReportedWhenRetry(@Nonnull final CapturedOutput output) {
        final String zoneName = stubErrorResponse();

        final EntityExchangeResult<LocalDateTime> result = webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("current-time")
                .build())
            .header("traceparent", "00-38c19768104ab8ae64fabbeed65bbbdf-4cac1747d4e1ee10-01")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(LocalDateTime.class)
            .returnResult();
        final String traceId = result.getResponseHeaders().getFirst(TRACE_ID_HEADER_NAME);
        assertThat(traceId)
            .isEqualTo("38c19768104ab8ae64fabbeed65bbbdf");

        assertThat(output.getAll())
            .containsPattern(String.format(Locale.ROOT,
                ".*INFO \\[spring-boot-2-demo-app,%s,[a-fA-F0-9]{16}] \\d+ --- \\[.*?] i\\.g\\.m\\.s\\.b\\.test\\.service\\.PublicApiService {2}: Retrying request to",
                traceId))
            .containsPattern(String.format(Locale.ROOT,
                ".*ERROR \\[spring-boot-2-demo-app,%s,[a-fA-F0-9]{16}] \\d+ --- \\[.*?] i\\.g\\.m\\.s\\.b\\.test\\.service\\.PublicApiService {2}: Request to '/%s' failed after 2 attempts",
                traceId, zoneName));
    }
}
