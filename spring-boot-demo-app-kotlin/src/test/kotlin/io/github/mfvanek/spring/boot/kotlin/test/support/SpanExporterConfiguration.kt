/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.kotlin.test.support

import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class SpanExporterConfiguration {
    @Bean
    @Primary
    fun spanExporter(): SpanExporter {
        return InMemorySpanExporter.create()
    }

    @Bean
    @Primary
    fun tracerProvider(spanExporter: SpanExporter): SdkTracerProvider {
        return SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build()
    }

    @Bean
    @Primary
    fun openTelemetrySdk(tracerProvider: SdkTracerProvider): OpenTelemetrySdk {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build()
    }
}
