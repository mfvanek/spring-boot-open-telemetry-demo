/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import javax.annotation.Nonnull;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaSendingService {

    private final KafkaTemplate<UUID, String> kafkaTemplate;
    @Value("${app.tenant.name}")
    private String tenantName;
    @Value("${spring.kafka.template.additional-topic}") private String additionalTopic;

    public Mono<SendResult<UUID, String>> sendNotification(@Nonnull final String message) {
        return Mono.deferContextual(contextView -> {
            try (MDC.MDCCloseable ignored = MDC.putCloseable("tenant.name", tenantName)) {
                log.info("Sending message \"{}\" to Kafka", message);
                return Mono.fromFuture(() -> kafkaTemplate.sendDefault(UUID.randomUUID(), message))
                    .subscribeOn(Schedulers.boundedElastic());
            }
        });
    }

    public Mono<SendResult<UUID, String>> sendNotificationToOtherTopic(@Nonnull final String message) {
        return Mono.deferContextual(contextView -> {
            try (MDC.MDCCloseable ignored = MDC.putCloseable("tenant.name", tenantName)) {
                log.info("Sending message \"{}\" to {} of Kafka", message, additionalTopic);
                return Mono.fromFuture(() -> kafkaTemplate.send(additionalTopic, UUID.randomUUID(), message))
                    .subscribeOn(Schedulers.boundedElastic());
            }
        });
    }
}
