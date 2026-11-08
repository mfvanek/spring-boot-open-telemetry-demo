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
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdInResponseReactiveAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TraceIdInResponseAutoConfiguration.class));

    @Test
    void shouldRegisterReactiveFilter() {
        contextRunner
            .run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(TraceIdInResponseReactiveFilter.class)
                .doesNotHaveBean(TraceIdInResponseServletFilter.class));
    }

    @Test
    void shouldNotRegisterServletFilterEvenWithTracer() {
        contextRunner
            .withBean(Tracer.class, () -> Tracer.NOOP)
            .run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(TraceIdInResponseReactiveFilter.class)
                .doesNotHaveBean(TraceIdInResponseServletFilter.class));
    }

    @Test
    void shouldNotRegisterReactiveFilterWhenDisabled() {
        contextRunner
            .withPropertyValues(TraceIdInResponseAutoConfiguration.ENABLED_PROPERTY + "=false")
            .run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(TraceIdInResponseAutoConfiguration.class)
                .doesNotHaveBean(TraceIdInResponseReactiveFilter.class));
    }

    @Test
    void shouldRegisterReactiveFilterWhenExplicitlyEnabled() {
        contextRunner
            .withPropertyValues(TraceIdInResponseAutoConfiguration.ENABLED_PROPERTY + "=true")
            .run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(TraceIdInResponseReactiveFilter.class));
    }

    @Test
    void shouldBackOffWithoutMicrometerTracing() {
        contextRunner
            .withClassLoader(new FilteredClassLoader(Tracer.class))
            .run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(TraceIdInResponseAutoConfiguration.class)
                .doesNotHaveBean(TraceIdInResponseReactiveFilter.class));
    }

    @Test
    void shouldBackOffWhenUserDefinesOwnReactiveFilter() {
        contextRunner
            .withUserConfiguration(CustomReactiveFilterConfiguration.class)
            .run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(TraceIdInResponseReactiveFilter.class)
                .getBean(TraceIdInResponseReactiveFilter.class)
                .isSameAs(context.getBean("customReactiveFilter")));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomReactiveFilterConfiguration {

        @Bean
        TraceIdInResponseReactiveFilter customReactiveFilter() {
            return new TraceIdInResponseReactiveFilter();
        }
    }
}
