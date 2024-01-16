package io.github.mfvanek.spring.boot3.test;

import io.github.mfvanek.spring.boot3.test.support.TestBase;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationTests extends TestBase {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext.containsBean("otlpMeterRegistry"))
                .isFalse();
        assertThat(applicationContext.getBean(ObservationRegistry.class))
                .isNotNull()
                .isInstanceOf(ObservationRegistry.class);
        assertThat(applicationContext.getBean(Tracer.class))
                .isNotNull()
                .isInstanceOf(OtelTracer.class)
                .satisfies(t -> assertThat(t.currentSpan())
                        .isNotEqualTo(Span.NOOP));
    }
}
