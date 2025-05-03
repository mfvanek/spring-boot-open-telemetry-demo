package io.github.mfvanek.spring.boot3.kotlin.test.controllers

import io.github.mfvanek.spring.boot3.kotlin.test.filters.TraceIdInResponseServletFilter.Companion.TRACE_ID_HEADER_NAME
import io.github.mfvanek.spring.boot3.kotlin.test.support.TestBase
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
            .responseBody;
        assertThat<Any>(result)
            .isNull()
    }
}
