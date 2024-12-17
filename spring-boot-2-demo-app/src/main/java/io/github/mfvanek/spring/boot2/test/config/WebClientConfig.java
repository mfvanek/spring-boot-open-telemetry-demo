package io.github.mfvanek.spring.boot2.test.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class WebClientConfig {

    @Bean
    WebClient webClient() {
        return WebClient.builder().build();
    }

}
