/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.kotlin.test.controllers

import io.github.mfvanek.spring.boot.kotlin.test.support.TestBase
import io.github.mfvanek.spring.boot.traceid.TraceIdResponseHeaders.TRACE_ID_HEADER_NAME
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HomeControllerTest : TestBase() {
    @Test
    fun homeControllerShouldWork() {
        val result = webTestClient.get()
            .uri("/")
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
        assertThat<String>(result)
            .isEqualTo("Hello!")
    }
}
