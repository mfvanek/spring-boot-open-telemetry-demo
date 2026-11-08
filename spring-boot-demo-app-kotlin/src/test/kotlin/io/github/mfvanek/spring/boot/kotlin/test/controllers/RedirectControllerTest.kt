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

class RedirectControllerTest : TestBase() {
    @Test
    fun redirectShouldWork() {
        val result = webTestClient.get()
            .uri("/redirect")
            .exchange()
            .expectStatus().isEqualTo(303)
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectHeader().location("https://www.google.com")
            .expectBody(Any::class.java)
            .returnResult()
            .responseBody
        assertThat<Any>(result)
            .isNull()
    }
}
