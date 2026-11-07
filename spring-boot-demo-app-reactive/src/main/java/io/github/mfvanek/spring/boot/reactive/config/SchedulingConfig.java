/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.reactive.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} methods.
 * <p>
 * Spring Boot automatically registers an {@code ObservationRegistry} for scheduled tasks
 * (see {@code ScheduledTasksObservationAutoConfiguration}), so each execution of a scheduled method
 * is wrapped in its own observation/span. As a result, every run gets a new traceId,
 * which is put into MDC and appears in all log messages written during that run.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {
}
