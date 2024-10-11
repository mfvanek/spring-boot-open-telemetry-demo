package io.github.mfvanek.spring.boot2.test;

import io.github.mfvanek.spring.boot2.test.support.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTests extends TestBase {

    @Test
    void contextLoads() {
    }

    @Test
    void jdbcQueryTimeoutFromProperties() {
        assertThat(jdbcTemplate.getQueryTimeout())
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Throws exception when query exceeds timeout")
    void exceptionWithLongQuery() {
        assertThatThrownBy(() -> jdbcTemplate.execute("select pg_sleep(1.1);"))
            .isInstanceOf(DataAccessResourceFailureException.class)
            .hasMessageContaining("ERROR: canceling statement due to user request");
    }

    @Test
    @DisplayName("Does not throw exception when query does not exceed timeout")
    void exceptionNotThrownWithNotLongQuery() {
        assertThatNoException().isThrownBy(() -> jdbcTemplate.execute("select pg_sleep(0.9);"));
    }
}
