package io.github.mfvanek.spring.boot2.test.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("resource")
public class JaegerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final DockerImageName IMAGE = DockerImageName.parse("jaegertracing/all-in-one:1.53");
    private static final GenericContainer<?> JAEGER = new GenericContainer<>(IMAGE)
        .withExposedPorts(4317);

    @Override
    public void initialize(final ConfigurableApplicationContext context) {
        JAEGER.start();

        final String jaegerUrl = "http://localhost:" + JAEGER.getFirstMappedPort();
        TestPropertyValues.of(
            "spring.sleuth.otel.exporter.otlp.endpoint=" + jaegerUrl
        ).applyTo(context.getEnvironment());
    }
}
