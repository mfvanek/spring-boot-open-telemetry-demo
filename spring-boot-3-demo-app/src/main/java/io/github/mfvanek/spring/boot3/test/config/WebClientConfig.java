package io.github.mfvanek.spring.boot3.test.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.external-base-url}")
    private String external;

    @Bean
    public WebClient webClient() {
        return WebClient.builder().baseUrl(external).build();
    }
}
