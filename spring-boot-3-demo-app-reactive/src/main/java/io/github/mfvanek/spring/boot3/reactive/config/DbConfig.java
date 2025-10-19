/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.config;

import io.github.mfvanek.db.migrations.common.saver.DbSaver;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class DbConfig {

    @Bean
    public DbSaver dbSaver(
        @Value("${app.tenant.name}") String tenantName,
        Tracer tracer,
        Clock clock,
        JdbcClient jdbcClient
    ) {
        return new DbSaver(tenantName, tracer, clock, jdbcClient);
    }
}
