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

class RedirectControllerTest extends TestBase {

    @Test
    void redirectShouldWork() {
        final Object result = webTestClient.get()
            .uri("/redirect")
            .exchange()
            .expectStatus().isEqualTo(303)
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectHeader().location("https://www.google.com")
            .expectBody(Object.class)
            .returnResult()
            .getResponseBody();
        assertThat(result)
            .isNull();
    }
}
