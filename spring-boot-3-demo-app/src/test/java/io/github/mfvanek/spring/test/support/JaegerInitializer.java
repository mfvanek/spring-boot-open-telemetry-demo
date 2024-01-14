package io.github.mfvanek.spring.test.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

public class JaegerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final DockerImageName IMAGE = DockerImageName.parse("jaegertracing/all-in-one:1.43");
    private static final Network NETWORK = Network.newNetwork();
    private static final GenericContainer<?> JAEGER = new GenericContainer<>(IMAGE)
            .withNetwork(NETWORK)
            .withExposedPorts(14250);

    @Override
    public void initialize(final ConfigurableApplicationContext context) {
        JAEGER.start();

        TestPropertyValues.of(
                "management.otlp.metrics.export.url=" + "http://localhost:" + JAEGER.getFirstMappedPort()
        ).applyTo(context.getEnvironment());
    }
}
