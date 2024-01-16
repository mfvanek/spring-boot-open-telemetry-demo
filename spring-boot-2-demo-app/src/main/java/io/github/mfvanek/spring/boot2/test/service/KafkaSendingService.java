package io.github.mfvanek.spring.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaSendingService {

    private final KafkaTemplate<UUID, String> kafkaTemplate;

    public CompletableFuture<SendResult<UUID, String>> sendNotification(@Nonnull final String message) {
        log.info("Sending message \"{}\" to Kafka", message);
        return kafkaTemplate.sendDefault(UUID.randomUUID(), message).completable();
    }
}
