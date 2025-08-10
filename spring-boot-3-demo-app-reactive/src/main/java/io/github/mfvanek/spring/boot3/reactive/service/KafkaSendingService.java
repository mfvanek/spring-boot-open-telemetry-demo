/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.annotation.Nonnull;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaSendingService {

    private final KafkaTemplate<UUID, String> kafkaTemplate;
    @Value("${app.tenant.name}")
    private String tenantName;
    //private final Propagator propagator;
    //private final Tracer tracer;

    public void sendNotification(@Nonnull final String message, @Nonnull final String traceId) {
        Mono.fromFuture(() -> {
            try (MDC.MDCCloseable ignored = MDC.putCloseable("tenant.name", tenantName)) {
                log.info("Sending message \"{}\" to Kafka", message);
                // это тоже удаляет
                //propagator.inject(
                //    Objects.requireNonNull(tracer.currentSpan()).context(),
                //    kafkaTemplate.getProducerFactory().getConfigurationProperties(),
                //    (stringObjectMap, key, value) -> stringObjectMap.put("traceparent", traceId)
                //);
                // это удаляет все
                // kafkaTemplate.getProducerFactory().getConfigurationProperties().remove("traceparent");
                //  kafkaTemplate.getProducerFactory().getConfigurationProperties().remove("b3");
                //   kafkaTemplate.getProducerFactory().getConfigurationProperties().putIfAbsent("traceparent", traceId);
                //   kafkaTemplate.getProducerFactory().getConfigurationProperties().putIfAbsent("b3", traceId);
                // так можно добавить хедер с другим названием, но существующие не заменяются
                final ProducerRecord<UUID, String> producerRecord = new ProducerRecord<>(
                    kafkaTemplate.getDefaultTopic(),
                    kafkaTemplate.partitionsFor(kafkaTemplate.getDefaultTopic()).get(0).partition(),
                    UUID.randomUUID(),
                    message,
                    new RecordHeaders(new Header[]{
                        new RecordHeader("traceparent", traceId.getBytes(StandardCharsets.UTF_8)),
                        new RecordHeader("b3", traceId.getBytes(StandardCharsets.UTF_8))})
                );
                return kafkaTemplate.send(producerRecord);
                //return kafkaTemplate.sendDefault(UUID.randomUUID(), message);
            }
        }).doOnError(e -> log.info("error ", e)).subscribe();
    }
}
