/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfvanek.spring.boot3.reactive.service.dto.CurrentTime;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicApiService {

    private final ObjectMapper mapper;
    private final WebClient webClient;
    @Value("${app.retries}")
    private int retries;

    public Mono<LocalDateTime> getZonedTime() {
        return getZonedTimeFromWorldTimeApi()
            .onErrorResume(ExhaustedRetryException.class,
                it -> {
                    log.warn("Failed to get response", it);
                    return Mono.empty();
                })
            .map(it -> it.getDatetime().toLocalDateTime());
    }

    private Mono<CurrentTime> getZonedTimeFromWorldTimeApi() {
        final String zoneName = TimeZone.getDefault().getID();
        return webClient.get()
            .uri(zoneName)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(prepareRetry(zoneName))
            .flatMap(this::convert)
            .onErrorComplete()
            .flatMap(Mono::justOrEmpty);
    }

    private Retry prepareRetry(final String zoneName) {
        return Retry.fixedDelay(retries, Duration.ofSeconds(2))
            .doBeforeRetry(retrySignal -> {
                try (MDC.MDCCloseable ignored = MDC.putCloseable("instance_timezone", zoneName)) {
                    log.info("Retrying request to '/{}', attempt {}/{} due to error:",
                        zoneName, retrySignal.totalRetries() + 1, retries, retrySignal.failure());
                }
            })
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                log.error("Request to '/{}' failed after {} attempts.", zoneName, retrySignal.totalRetries() + 1);
                return new ExhaustedRetryException("Retries exhausted", retrySignal.failure());
            });
    }

    @SneakyThrows
    private Mono<? extends CurrentTime> convert(String string) {
        return Mono.just(mapper.readValue(string, CurrentTime.class));
    }
}
