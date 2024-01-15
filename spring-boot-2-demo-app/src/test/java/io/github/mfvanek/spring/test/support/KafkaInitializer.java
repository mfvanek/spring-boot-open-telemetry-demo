package io.github.mfvanek.spring.test.support;

import org.apache.kafka.common.security.plain.PlainLoginModule;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.stream.Collectors;

public class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String KAFKA_USER_NAME = "sb-ot-demo-user";
    private static final String KAFKA_USER_PASSWORD = "pwdForSbOtDemoApp";

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka:7.5.3");

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(IMAGE_NAME)
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SASL_PLAINTEXT,BROKER:PLAINTEXT")
            .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
            .withEnv("KAFKA_SASL_JAAS_CONFIG", plainJaas(Map.of()))
            .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG", plainJaas(Map.of(KAFKA_USER_NAME, KAFKA_USER_PASSWORD)));

    private static String plainJaas(@Nonnull final Map<String, String> additionalUsers) {
        final String users = additionalUsers.entrySet()
                .stream()
                .map(e -> "user_%s=\"%s\"".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(" "));
        final StringBuilder builder = new StringBuilder()
                .append(PlainLoginModule.class.getName())
                .append(" required username=\"%s\" password=\"%s\"".formatted(KAFKA_USER_NAME, KAFKA_USER_PASSWORD));
        if (!users.isBlank()) {
            builder.append(" ")
                    .append(users);
        }
       return builder.append(";")
                .toString();
    }

    @Override
    public void initialize(@Nonnull final ConfigurableApplicationContext applicationContext) {
        KAFKA_CONTAINER.start();
        TestPropertyValues.of(
                "spring.kafka.bootstrap-servers=" + KAFKA_CONTAINER.getBootstrapServers(),
                "demo.kafka.opentelemetry.username=" + KAFKA_USER_NAME,
                "demo.kafka.opentelemetry.password=" + KAFKA_USER_PASSWORD
        ).applyTo(applicationContext.getEnvironment());
    }
}
