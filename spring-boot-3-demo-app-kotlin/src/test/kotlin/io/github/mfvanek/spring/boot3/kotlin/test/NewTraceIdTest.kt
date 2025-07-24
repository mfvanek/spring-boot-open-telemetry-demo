package io.github.mfvanek.spring.boot3.kotlin.test

import io.github.mfvanek.spring.boot3.kotlin.test.support.TestBase
import io.micrometer.tracing.Tracer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class NewTraceIdTest : TestBase() {

    @Autowired
    private lateinit var tracer: Tracer

    @Suppress("NestedBlockDepth")
    @DisplayName("Demonstration of how to create a new traceId using the span API")
    @Test
    fun canCreateNewTraceIdViaSpan() {
        val firstSpan = tracer.startScopedSpan("first")
        try {
            val previousTraceId = tracer.currentSpan()?.context()?.traceId()
            assertThat(previousTraceId)
                .isEqualTo(firstSpan.context().traceId())

            tracer.withSpan(null).use {
                val newSpan = tracer.nextSpan().name("new")
                try {
                    tracer.withSpan(newSpan.start()).use {
                        val newTraceId = tracer.currentSpan()?.context()?.traceId()
                        assertThat(newTraceId)
                            .isNotEqualTo(previousTraceId)
                    }
                } catch (e: Exception) {
                    newSpan.error(e)
                    throw e
                } finally {
                    newSpan.end()
                }
            }

            val lastTraceId = tracer.currentSpan()?.context()?.traceId()
            assertThat(lastTraceId)
                .isEqualTo(previousTraceId)
        } catch (e: Exception) {
            firstSpan.error(e)
            throw e
        } finally {
            firstSpan.end()
        }
    }

    @DisplayName("Demonstration of creating a new traceId using the API traceContext")
    @Test
    @Suppress("NestedBlockDepth")
    fun canCreateNewTraceIdViaContext() {
        val firstSpan = tracer.startScopedSpan("first")
        try {
            val previousTraceId = tracer.currentSpan()?.context()?.traceId()
            assertThat(previousTraceId)
                .isEqualTo(firstSpan.context().traceId())

            tracer.withSpan(null).use {
                val newSpan = tracer.nextSpan()
                val traceContext = tracer.traceContextBuilder()
                    .traceId(newSpan.context().traceId())
                    .spanId(newSpan.context().spanId())
                    .sampled(true) // Обязательно!
                    .build()
                tracer.currentTraceContext().newScope(
                    traceContext
                ).use { // Scope из newScope обязательно нужно закрывать
                    val newTraceId = tracer.currentSpan()?.context()?.traceId()
                    assertThat(newTraceId)
                        .isNotEqualTo(previousTraceId)
                }
            }

            val lastTraceId = tracer.currentSpan()?.context()?.traceId()
            assertThat(lastTraceId)
                .isEqualTo(previousTraceId)
        } catch (e: Exception) {
            firstSpan.error(e)
            throw e
        } finally {
            firstSpan.end()
        }
    }
}
