/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.test.service;

import io.github.mfvanek.spring.boot.core.RetriesExhaustedException;
import io.github.mfvanek.spring.boot.test.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot.test.service.dto.ParsedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicApiService {

    @Value("${app.retries}")
    private int retries;
    @Value("${app.retry-delay}")
    private Duration retryDelay;

    private final ObjectMapper mapper;
    private final WebClient webClient;

    @Nullable
    public LocalDateTime getZonedTime() {
        try {
            final ParsedDateTime result = getZonedTimeFromWorldTimeApi().getDatetime();
            return result.toLocalDateTime();
        } catch (RetriesExhaustedException e) {
            log.warn("Failed to get response", e);
        } catch (JacksonException e) {
            log.warn("Failed to convert response", e);
        }
        return null;
    }

    private CurrentTime getZonedTimeFromWorldTimeApi() {
        final String zoneName = TimeZone.getDefault().getID();
        final Mono<String> response = webClient.get()
            .uri(zoneName)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(prepareRetry(zoneName));
        return mapper.readValue(response.block(), CurrentTime.class);
    }

    private Retry prepareRetry(final String zoneName) {
        return Retry.fixedDelay(retries, retryDelay)
            .doBeforeRetry(retrySignal -> {
                try (MDC.MDCCloseable ignored = MDC.putCloseable("instance_timezone", zoneName)) {
                    log.info("Retrying request to '/{}', attempt {}/{} due to error:",
                        zoneName, retrySignal.totalRetries() + 1, retries, retrySignal.failure());
                }
            })
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                log.error("Request to '/{}' failed after {} attempts.", zoneName, retrySignal.totalRetries() + 1);
                return new RetriesExhaustedException("Retries exhausted", retrySignal.failure());
            });
    }
}
