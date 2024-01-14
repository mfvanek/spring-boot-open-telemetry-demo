package io.github.mfvanek.spring.test.controllers;

import io.github.mfvanek.spring.test.support.TestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class TimeControllerTest extends TestBase {

    @Test
    void spanShouldBeReportedInLogs(@Nonnull final CapturedOutput output) {
        final var result = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("current-time")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(LocalDateTime.class)
                .returnResult()
                .getResponseBody();
        assertThat(result)
                .isBefore(LocalDateTime.now());
        assertThat(output.getAll())
                .contains("Called method getNow. TraceId = ");
    }
}
