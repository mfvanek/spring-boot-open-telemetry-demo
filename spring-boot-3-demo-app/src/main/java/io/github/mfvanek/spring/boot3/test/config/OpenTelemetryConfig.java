/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.config;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import javax.annotation.Nonnull;

@AutoConfigureBefore(OtlpAutoConfiguration.class)
@Configuration(proxyBeanMethods = false)
class OpenTelemetryConfig {

    // Waiting for https://github.com/spring-projects/spring-boot/pull/41213
    @Bean
    @ConditionalOnMissingBean(OtlpGrpcSpanExporter.class)
    OtlpGrpcSpanExporter otelJaegerGrpcSpanExporter(@Nonnull final OtlpProperties otlpProperties) {
        final OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
            .setEndpoint(otlpProperties.getEndpoint())
            .setTimeout(otlpProperties.getTimeout())
            .setCompression(String.valueOf(otlpProperties.getCompression()).toLowerCase(Locale.ROOT));
        otlpProperties.getHeaders().forEach(builder::addHeader);
        return builder.build();
    }
}
