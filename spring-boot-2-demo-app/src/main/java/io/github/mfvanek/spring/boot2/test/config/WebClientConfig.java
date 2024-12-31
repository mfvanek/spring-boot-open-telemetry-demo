package io.github.mfvanek.spring.boot2.test.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Value("${app.external-base-url}")
    private String external;

    @Bean
    WebClient webClient() {
        return WebClient.builder().baseUrl(external).build();
    }
}
