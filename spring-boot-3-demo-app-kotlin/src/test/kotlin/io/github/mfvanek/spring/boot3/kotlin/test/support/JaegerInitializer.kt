package io.github.mfvanek.spring.boot3.kotlin.test.support

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
            "management.otlp.tracing.endpoint=$jaegerUrl"
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
