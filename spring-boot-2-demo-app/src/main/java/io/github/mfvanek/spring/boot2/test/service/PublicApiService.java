package io.github.mfvanek.spring.boot2.test.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfvanek.spring.boot2.test.service.dto.CurrentTime;
import io.github.mfvanek.spring.boot2.test.service.dto.ParsedDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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
@Getter
public class PublicApiService {
    @Value("${app.retries}")
    private String retries;
    private final ObjectMapper mapper;
    private final WebClient webClient;

    public LocalDateTime getZonedTime() {
        try {
            final ParsedDateTime result = getZonedTimeFromWorldTimeApi().getDatetime();
            return LocalDateTime.of(result.getYear(), result.getMonthValue(), result.getDayOfMonth(), result.getHour(), result.getMinute());
        } catch (RuntimeException e) {
            log.warn("Failed to get response ", e);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert response ", e);
        }
        return null;
    }

    private CurrentTime getZonedTimeFromWorldTimeApi() throws JsonProcessingException {
        final String zoneNames = TimeZone.getDefault().getID();
        Mono<String> response = webClient.get()
            .uri(String.join("/", zoneNames))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                .doBeforeRetry(retrySignal -> log.info("Retrying request to {}, attempt {}/{} due to error:",
                    webClient.options().uri(String.join("", zoneNames)), retries, retrySignal.totalRetries() + 1, retrySignal.failure()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Request to {} failed after {} attempts.", webClient.options().uri(String.join("", zoneNames)), retrySignal.totalRetries() + 1);
                    return new RuntimeException("Retries exhausted", retrySignal.failure());
                })
            );
        return mapper.readValue(response.block(), CurrentTime.class);
    }
}
