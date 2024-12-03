package io.github.mfvanek.spring.boot2.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.TimeZone;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicApiService {

    private final WebClient webClient;

    public String getZonedTime() {
        final String zoneNames = TimeZone.getDefault().getID();
        final String url = "http://worldtimeapi.org/api/timezone/" + zoneNames;
        String answer;
        try {
            answer = getZonedTimeFromWorldTimeApi(url);
        } catch (RuntimeException e) {
            answer = e.getMessage();
        }
        return answer;
    }

    @SuppressWarnings("Slf4jDoNotLogMessageOfExceptionExplicitly")
    private String getZonedTimeFromWorldTimeApi(String url) {
        Mono<String> answer = webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                .doBeforeRetry(retrySignal -> log.info("Retrying request to {}, attempt {}/3 due to error: {}",
                    url, retrySignal.totalRetries() + 1, retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Request to {} failed after {} attempts.", url, retrySignal.totalRetries() + 1);
                    return new RuntimeException("Retries exhausted", retrySignal.failure());
                })
            );
        return answer.block();
    }
}
