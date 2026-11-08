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
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

/**
 * Auto-configuration that adds the traceId of the current request to every HTTP response.
 * <p>
 * Registers {@link TraceIdInResponseServletFilter} in servlet web applications
 * and {@link TraceIdInResponseReactiveFilter} in reactive web applications.
 * Can be switched off with {@code demo.trace-id-in-response.enabled=false}.
 */
@AutoConfiguration(afterName = {
    "org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration",
    "org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryTracingAutoConfiguration"
})
@ConditionalOnClass(Tracer.class)
@ConditionalOnBooleanProperty(name = TraceIdInResponseAutoConfiguration.ENABLED_PROPERTY, matchIfMissing = true)
@SuppressWarnings("PMD.InstantiableUtilityClass") // Spring instantiates auto-configuration classes
public class TraceIdInResponseAutoConfiguration {

    /**
     * Property that enables or disables adding the traceId to responses.
     */
    public static final String ENABLED_PROPERTY = "demo.trace-id-in-response.enabled";

    /**
     * Servlet stack configuration.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(Filter.class)
    @ConditionalOnBean(Tracer.class)
    static class ServletConfiguration {

        @Bean
        @ConditionalOnMissingBean
        TraceIdInResponseServletFilter traceIdInResponseServletFilter(final Tracer tracer) {
            return new TraceIdInResponseServletFilter(tracer);
        }
    }

    /**
     * Reactive stack configuration.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(WebFilter.class)
    static class ReactiveConfiguration {

        @Bean
        @ConditionalOnMissingBean
        TraceIdInResponseReactiveFilter traceIdInResponseReactiveFilter() {
            return new TraceIdInResponseReactiveFilter();
        }
    }
}
