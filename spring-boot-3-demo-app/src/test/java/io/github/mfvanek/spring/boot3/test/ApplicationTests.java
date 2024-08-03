package io.github.mfvanek.spring.boot3.test;

import io.github.mfvanek.spring.boot3.test.support.JaegerInitializer;
import io.github.mfvanek.spring.boot3.test.support.TestBase;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Locale;

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
        assertThat(applicationContext.getBean("otelJaegerGrpcSpanExporter"))
                .isNotNull()
                .isInstanceOf(OtlpGrpcSpanExporter.class)
                .hasToString(String.format(Locale.ROOT, "OtlpGrpcSpanExporter{exporterName=otlp, type=span, " +
                        "endpoint=http://localhost:%d, " +
                        "endpointPath=/opentelemetry.proto.collector.trace.v1.TraceService/Export, timeoutNanos=5000000000, " +
                        "connectTimeoutNanos=10000000000, compressorEncoding=null, " +
                        "headers=Headers{User-Agent=OBFUSCATED}}", JaegerInitializer.getFirstMappedPort()));
    }
}
