package io.github.mfvanek.spring.boot3.test.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class KafkaReadingService {

    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void listen(ConsumerRecord<UUID, String> message, Acknowledgment ack) {
        log.info("Received record: {}", message);
        ack.acknowledge();
    }
}
