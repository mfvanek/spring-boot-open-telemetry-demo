package io.github.mfvanek.spring.boot2.test.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {KafkaInitializer.class, JaegerInitializer.class})
@ActiveProfiles("test")
public abstract class TestBase {

    @Autowired
    protected WebTestClient webTestClient;
}
