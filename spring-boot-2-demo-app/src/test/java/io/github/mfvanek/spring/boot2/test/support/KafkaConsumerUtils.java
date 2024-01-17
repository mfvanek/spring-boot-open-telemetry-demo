package io.github.mfvanek.spring.boot2.test.support;

import lombok.experimental.UtilityClass;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

@UtilityClass
public class KafkaConsumerUtils {

    public KafkaMessageListenerContainer<UUID, String> setUpKafkaConsumer(
            @Nonnull final KafkaProperties kafkaProperties,
            @Nonnull final BlockingQueue<ConsumerRecord<UUID, String>> consumerRecords) {
        final var containerProperties = new ContainerProperties(kafkaProperties.getTemplate().getDefaultTopic());
        final Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(KafkaInitializer.getBootstrapSevers(), "sender", "false");
        consumerProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        consumerProperties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        consumerProperties.put(SaslConfigs.SASL_JAAS_CONFIG, KafkaInitializer.plainJaas());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.UUIDDeserializer.class);
        final var consumer = new DefaultKafkaConsumerFactory<UUID, String>(consumerProperties);
        final var container = new KafkaMessageListenerContainer<>(consumer, containerProperties);
        container.setupMessageListener((MessageListener<UUID, String>) consumerRecords::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, 1);
        return container;
    }
}
