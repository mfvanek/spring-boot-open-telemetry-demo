/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.controllers;

import io.github.mfvanek.spring.boot3.reactive.support.TestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest extends TestBase {

    @Test
    void controllerResponseShouldHaveTraceIdHeader() {
        final String result = webTestClient.get()
            .uri("/")
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        assertThat(result)
            .isEqualTo("Hello!");
    }
}
