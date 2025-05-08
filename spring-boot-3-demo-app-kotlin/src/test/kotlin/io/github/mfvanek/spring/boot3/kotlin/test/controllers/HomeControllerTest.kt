package io.github.mfvanek.spring.boot3.kotlin.test.controllers

import io.github.mfvanek.spring.boot3.kotlin.test.filters.TraceIdInResponseServletFilter.Companion.TRACE_ID_HEADER_NAME
import io.github.mfvanek.spring.boot3.kotlin.test.support.TestBase
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
