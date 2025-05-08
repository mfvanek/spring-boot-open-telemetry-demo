package io.github.mfvanek.spring.boot3.kotlin.test

import io.github.mfvanek.spring.boot3.kotlin.test.support.JaegerInitializer
import io.github.mfvanek.spring.boot3.kotlin.test.support.TestBase
import io.micrometer.observation.ObservationRegistry
import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.otel.bridge.OtelTracer
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import org.assertj.core.api.Assertions.*
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.dao.DataAccessResourceFailureException
import java.util.*

class ApplicationTests : TestBase() {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun contextLoads() {
        assertThat(applicationContext.containsBean("otlpMeterRegistry"))
            .isFalse()
        assertThat(applicationContext.getBean(ObservationRegistry::class.java))
            .isNotNull()
            .isInstanceOf(ObservationRegistry::class.java)
        assertThat(applicationContext.getBean(Tracer::class.java))
            .isNotNull()
            .isInstanceOf(OtelTracer::class.java)
            .satisfies(ThrowingConsumer { t: Tracer ->
                assertThat(t.currentSpan())
                    .isNotEqualTo(Span.NOOP)
            })
        assertThat(applicationContext.getBean("otelJaegerGrpcSpanExporter"))
            .isNotNull()
            .isInstanceOf(OtlpGrpcSpanExporter::class.java)
            .hasToString(
                String.format(
                    Locale.ROOT, """
                OtlpGrpcSpanExporter{exporterName=otlp, type=span, endpoint=http://localhost:%d, endpointPath=/opentelemetry.proto.collector.trace.v1.TraceService/Export, timeoutNanos=5000000000, connectTimeoutNanos=10000000000, compressorEncoding=null, headers=Headers{User-Agent=OBFUSCATED}, retryPolicy=RetryPolicy{maxAttempts=5, initialBackoff=PT1S, maxBackoff=PT5S, backoffMultiplier=1.5}, memoryMode=IMMUTABLE_DATA}
                """.trimIndent(), JaegerInitializer.getFirstMappedPort()
                )
            )
    }

    @Test
    fun jdbcQueryTimeoutFromProperties() {
        assertThat(jdbcTemplate.queryTimeout)
            .isEqualTo(1)
    }

    @Test
    @DisplayName("Throws exception when query exceeds timeout")
    fun exceptionWithLongQuery() {
        assertThatThrownBy { jdbcTemplate.execute("select pg_sleep(1.1);") }
            .isInstanceOf(DataAccessResourceFailureException::class.java)
            .hasMessageContaining("ERROR: canceling statement due to user request")
    }

    @Test
    @DisplayName("Does not throw exception when query does not exceed timeout")
    fun exceptionNotThrownWithNotLongQuery() {
        assertThatNoException().isThrownBy { jdbcTemplate.execute("select pg_sleep(0.9);") }
    }
}
