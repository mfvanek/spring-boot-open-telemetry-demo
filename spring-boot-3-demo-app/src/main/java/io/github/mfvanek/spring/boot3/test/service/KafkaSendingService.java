package io.github.mfvanek.spring.boot3.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaSendingService {

    @Value("${tenant.name}")
    private String tenantName;
    private final KafkaTemplate<UUID, String> kafkaTemplate;

    public CompletableFuture<SendResult<UUID, String>> sendNotification(@Nonnull final String message) {
        try (var ignored = MDC.putCloseable("tenant.name", tenantName)) {
            log.info("Sending message \"{}\" to Kafka", message);
            return kafkaTemplate.sendDefault(UUID.randomUUID(), message);
        }
    }
}
