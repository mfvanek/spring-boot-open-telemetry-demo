package io.github.mfvanek.spring.boot2.test.service;

import io.github.mfvanek.spring.boot2.test.support.TestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class PublicApiServiceTest extends TestBase {
    @Autowired
    PublicApiService publicApiService;
    @Test
    void printTimeZone(){
        final String result = publicApiService.getZonedTime();

        assertThat(result).isNotBlank();
    }

}
