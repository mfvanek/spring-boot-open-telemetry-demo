package io.github.mfvanek.spring.boot2.test.service;

import io.github.mfvanek.spring.boot2.test.dto.CurrentTime;


import java.net.URI;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class PublicApiService {

    @Value("${app.external-base-url}")
    private String external;

    private final WebClient webClient;

    public LocalDateTime getZonedTime() {
        LocalDateTime answer;
        try {
            answer = getZonedTimeFromWorldTimeApi().getDatetime();
        } catch (RuntimeException e) {
            answer = null;
        }
        return answer;
    }

    private CurrentTime getZonedTimeFromWorldTimeApi() {
        final String zoneNames = TimeZone.getDefault().getID();
        final String uri = UriComponentsBuilder.newInstance()
            .uri(URI.create(external))
            .pathSegment(zoneNames)
            .build()
            .toUriString();
        Mono<CurrentTime> answer = webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(CurrentTime.class)
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                .doBeforeRetry(retrySignal -> log.info("Retrying request to {}, attempt {}/3 due to error:",
                    external, retrySignal.totalRetries() + 1, retrySignal.failure()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Request to {} failed after {} attempts.", external, retrySignal.totalRetries() + 1);
                    return new RuntimeException("Retries exhausted", retrySignal.failure());
                })
            );
        return answer.block();
    }
}
