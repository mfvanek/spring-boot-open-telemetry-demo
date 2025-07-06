/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.kotlin.test.config

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder
import io.opentelemetry.sdk.common.export.RetryPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpGrpcSpanExporterBuilderCustomizer
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfigureBefore(OtlpTracingAutoConfiguration::class)
@Configuration(proxyBeanMethods = false)
class OpenTelemetryConfig {

    @Bean
    fun otelJaegerGrpcSpanExporterBuilderCustomizer(
        @Value("\${management.otlp.tracing.retry.max-attempts:2}") maxAttempts: Int
    ): OtlpGrpcSpanExporterBuilderCustomizer {
        return OtlpGrpcSpanExporterBuilderCustomizer { builder: OtlpGrpcSpanExporterBuilder ->
            builder.setRetryPolicy(
                RetryPolicy.builder()
                    .setMaxAttempts(maxAttempts)
                    .build()
            )
        }
    }
}
