package io.github.mfvanek.spring.boot3.test.config;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nonnull;

@Configuration(proxyBeanMethods = false)
public class OpenTelemetryConfig {

    @Bean
    OtlpGrpcSpanExporter otelJaegerGrpcSpanExporter(
            @Value("${management.otlp.metrics.export.url}") final @Nonnull String endpoint) {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();
    }
}
