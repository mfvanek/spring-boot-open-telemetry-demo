/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.kotlin.test.config

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import jakarta.annotation.Nonnull
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingProperties
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfigureBefore(OtlpTracingAutoConfiguration::class)
@Configuration(proxyBeanMethods = false)
class OpenTelemetryConfig {
    @Bean
    @ConditionalOnMissingBean(OtlpGrpcSpanExporter::class)
    fun otelJaegerGrpcSpanExporter(@Nonnull otlpProperties: OtlpTracingProperties): OtlpGrpcSpanExporter {
        val builder = OtlpGrpcSpanExporter.builder()
            .setEndpoint(otlpProperties.endpoint)
            .setTimeout(otlpProperties.timeout)
            .setConnectTimeout(otlpProperties.connectTimeout)
            .setCompression(otlpProperties.compression.toString().lowercase())
        otlpProperties.headers.forEach { (key, value) -> builder.addHeader(key, value) }
        return builder.build()
    }
}
