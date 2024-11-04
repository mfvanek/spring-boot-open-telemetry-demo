package io.github.mfvanek.spring.boot3.test.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class PostgresInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:17.0");
    private static final Network NETWORK = Network.newNetwork();
    private static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>(IMAGE);

    @Override
    public void initialize(final ConfigurableApplicationContext context) {
        CONTAINER
            .withNetwork(NETWORK)
            .withUsername("otel_demo_user")
            .withPassword("otel_demo_password")
            .withUrlParam("prepareThreshold", "0")
            .waitingFor(Wait.forListeningPort())
            .start();

        TestPropertyValues.of(
            "spring.datasource.url=" + CONTAINER.getJdbcUrl(),
            "spring.datasource.username=" + CONTAINER.getUsername(),
            "spring.datasource.password=" + CONTAINER.getPassword()
        ).applyTo(context.getEnvironment());
    }
}
