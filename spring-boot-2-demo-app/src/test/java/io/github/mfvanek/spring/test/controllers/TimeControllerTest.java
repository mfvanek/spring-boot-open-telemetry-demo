package io.github.mfvanek.spring.test.controllers;

import io.github.mfvanek.spring.test.support.KafkaInitializer;
import io.github.mfvanek.spring.test.support.TestBase;
import lombok.SneakyThrows;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimeControllerTest extends TestBase {

    private KafkaMessageListenerContainer<UUID, String> container;
    private final BlockingQueue<ConsumerRecord<UUID, String>> consumerRecords = new LinkedBlockingQueue<>();

    @Autowired
    private KafkaProperties kafkaProperties;

    @BeforeAll
    void setUpKafkaConsumer() {
        final var containerProperties = new ContainerProperties(kafkaProperties.getTemplate().getDefaultTopic());
        final Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(KafkaInitializer.getBootstrapSevers(), "sender", "false");
        consumerProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        consumerProperties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        consumerProperties.put(SaslConfigs.SASL_JAAS_CONFIG, KafkaInitializer.plainJaas());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.UUIDDeserializer.class);
        final var consumer = new DefaultKafkaConsumerFactory<UUID, String>(consumerProperties);
        container = new KafkaMessageListenerContainer<>(consumer, containerProperties);
        container.setupMessageListener((MessageListener<UUID, String>) consumerRecords::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, 1);
    }

    @AfterAll
    void tearDownKafkaConsumer() {
        if (container != null) {
            container.stop();
            container = null;
        }
    }

    @SneakyThrows
    @Test
    void spanShouldBeReportedInLogs(@Nonnull final CapturedOutput output) {
        final var result = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("current-time")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(LocalDateTime.class)
                .returnResult()
                .getResponseBody();
        assertThat(result)
                .isBefore(LocalDateTime.now());
        assertThat(output.getAll())
                .contains("Called method getNow. TraceId = ")
                .contains("Awaiting acknowledgement from Kafka");

        final var received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.value()).startsWith("Current time = ");
        final List<String> headerNames = Arrays.stream(received.headers().toArray())
                .map(Header::key)
                .toList();
        assertThat(headerNames)
                .hasSize(2)
                .containsExactlyInAnyOrder("traceparent", "b3");
    }
}
