/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot2.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfvanek.spring.boot2.test.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot2.test.service.dto.ParsedDateTime;
import lombok.RequiredArgsConstructor;
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
import javax.annotation.Nullable;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicApiService {

    @Value("${app.retries}")
    private int retries;

    private final ObjectMapper mapper;
    private final WebClient webClient;

    @Nullable
    public LocalDateTime getZonedTime() {
        try {
            final ParsedDateTime result = getZonedTimeFromWorldTimeApi().getDatetime();
            return result.toLocalDateTime();
        } catch (ExhaustedRetryException e) {
            log.warn("Failed to get response", e);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert response", e);
        }
        return null;
    }

    private CurrentTime getZonedTimeFromWorldTimeApi() throws JsonProcessingException {
        final String zoneNames = TimeZone.getDefault().getID();
        final Mono<String> response = webClient.get()
            .uri(String.join("/", zoneNames))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(Retry.fixedDelay(retries, Duration.ofSeconds(2))
                .doBeforeRetry(retrySignal -> {
                    try (MDC.MDCCloseable ignored = MDC.putCloseable("instance_timezone", zoneNames)) {
                        log.info("Retrying request to {}, attempt {}/{} due to error:",
                            webClient.options().uri(String.join("", zoneNames)), retrySignal.totalRetries() + 1, retries, retrySignal.failure());
                    }
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Request to {} failed after {} attempts.", webClient.options().uri(String.join("", zoneNames)), retrySignal.totalRetries() + 1);
                    return new ExhaustedRetryException("Retries exhausted", retrySignal.failure());
                })
            );
        return mapper.readValue(response.block(), CurrentTime.class);
    }
}
