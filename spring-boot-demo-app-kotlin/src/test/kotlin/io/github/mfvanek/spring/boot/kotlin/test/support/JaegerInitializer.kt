/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.kotlin.test.support

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class JaegerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(context: ConfigurableApplicationContext) {
        JAEGER.start()
        val jaegerUrl = "http://localhost:" + JAEGER.firstMappedPort
        TestPropertyValues.of(
            "management.opentelemetry.tracing.export.otlp.endpoint=$jaegerUrl"
        ).applyTo(context.environment)
    }

    companion object {
        @JvmStatic
        private val IMAGE: DockerImageName = DockerImageName.parse("jaegertracing/all-in-one:1.53")

        @JvmStatic
        private val JAEGER = GenericContainer(IMAGE).withExposedPorts(4317)

        @JvmStatic
        fun getFirstMappedPort(): Int = JAEGER.firstMappedPort
    }
}
