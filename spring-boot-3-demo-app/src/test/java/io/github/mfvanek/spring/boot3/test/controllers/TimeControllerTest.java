package io.github.mfvanek.spring.boot3.test.controllers;

import io.github.mfvanek.spring.boot3.test.support.TestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.time.LocalDateTime;

import static io.github.mfvanek.spring.boot3.test.filters.TraceIdInResponseServletFilter.TRACE_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class TimeControllerTest extends TestBase {

    @Autowired
    private Clock clock;

    @Test
    void spanShouldBeReportedInLogs(@Nonnull final CapturedOutput output) {
        final var result = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("current-time")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(TRACE_ID_HEADER_NAME)
                .expectBody(LocalDateTime.class)
                .returnResult();
        final String traceId = result.getResponseHeaders().getFirst(TRACE_ID_HEADER_NAME);
        assertThat(traceId).isNotBlank();
        assertThat(result.getResponseBody())
                .isBefore(LocalDateTime.now(clock));
        assertThat(output.getAll())
                .contains("Called method getNow. TraceId = " + traceId);
    }
}
