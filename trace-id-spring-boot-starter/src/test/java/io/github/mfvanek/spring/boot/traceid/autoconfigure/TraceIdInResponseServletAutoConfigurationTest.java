/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.traceid.autoconfigure;

import io.github.mfvanek.spring.boot.traceid.reactive.TraceIdInResponseReactiveFilter;
import io.github.mfvanek.spring.boot.traceid.servlet.TraceIdInResponseServletFilter;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdInResponseServletAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TraceIdInResponseAutoConfiguration.class));

    @Test
    void shouldRegisterServletFilterWhenTracerExists() {
        contextRunner
            .withBean(Tracer.class, () -> Tracer.NOOP)
            .run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(TraceIdInResponseServletFilter.class)
                .doesNotHaveBean(TraceIdInResponseReactiveFilter.class));
    }

    @Test
    void shouldRegisterServletFilterAfterTracerFromSpringBootAutoConfiguration() {
        // Our auto-configuration goes first on purpose: ordering must come from @AutoConfiguration(afterName = ...)
        new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                TraceIdInResponseAutoConfiguration.class,
                OpenTelemetryTracingAutoConfiguration.class,
                MicrometerTracingAutoConfiguration.class,
                OpenTelemetrySdkAutoConfiguration.class))
            .run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(Tracer.class)
                .hasSingleBean(TraceIdInResponseServletFilter.class));
    }

    @Test
    void shouldNotRegisterServletFilterWithoutTracer() {
        contextRunner
            .run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(TraceIdInResponseServletFilter.class)
                .doesNotHaveBean(TraceIdInResponseReactiveFilter.class));
    }

    @Test
    void shouldNotRegisterFiltersInNonWebApplication() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TraceIdInResponseAutoConfiguration.class))
            .withBean(Tracer.class, () -> Tracer.NOOP)
            .run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(TraceIdInResponseServletFilter.class)
                .doesNotHaveBean(TraceIdInResponseReactiveFilter.class));
    }

    @Test
    void shouldNotRegisterServletFilterWhenDisabled() {
        contextRunner
            .withBean(Tracer.class, () -> Tracer.NOOP)
            .withPropertyValues(TraceIdInResponseAutoConfiguration.ENABLED_PROPERTY + "=false")
            .run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(TraceIdInResponseAutoConfiguration.class)
                .doesNotHaveBean(TraceIdInResponseServletFilter.class));
    }

    @Test
    void shouldRegisterServletFilterWhenExplicitlyEnabled() {
        contextRunner
            .withBean(Tracer.class, () -> Tracer.NOOP)
            .withPropertyValues(TraceIdInResponseAutoConfiguration.ENABLED_PROPERTY + "=true")
            .run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(TraceIdInResponseServletFilter.class));
    }

    @Test
    void shouldBackOffWithoutMicrometerTracing() {
        contextRunner
            .withClassLoader(new FilteredClassLoader(Tracer.class))
            .run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(TraceIdInResponseAutoConfiguration.class)
                .doesNotHaveBean(TraceIdInResponseServletFilter.class));
    }

    @Test
    void shouldBackOffWhenUserDefinesOwnServletFilter() {
        contextRunner
            .withUserConfiguration(CustomServletFilterConfiguration.class)
            .run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(TraceIdInResponseServletFilter.class)
                .getBean(TraceIdInResponseServletFilter.class)
                .isSameAs(context.getBean("customServletFilter")));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomServletFilterConfiguration {

        @Bean
        Tracer tracer() {
            return Tracer.NOOP;
        }

        @Bean
        TraceIdInResponseServletFilter customServletFilter(final Tracer tracer) {
            return new TraceIdInResponseServletFilter(tracer);
        }
    }
}
