/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.controllers;

import io.github.mfvanek.spring.boot3.test.support.TestBase;
import org.junit.jupiter.api.Test;

import static io.github.mfvanek.spring.boot3.test.filters.TraceIdInResponseServletFilter.TRACE_ID_HEADER_NAME;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class HomeControllerTest extends TestBase {

    @Test
    void homeControllerShouldWork() {
        final String result = webTestClient.get()
            .uri("/")
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
        assertThat(result).isEqualTo("Hello!");
    }
}
