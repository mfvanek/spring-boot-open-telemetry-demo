package io.github.mfvanek.spring.test.config;

import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nonnull;

@Configuration(proxyBeanMethods = false)
public class OpenTelemetryConfig {

    @Bean
    JaegerGrpcSpanExporter otelJaegerGrpcSpanExporter(
            @Value("${management.otlp.metrics.export.url}") final @Nonnull String endpoint) {
        return JaegerGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();
    }
}
