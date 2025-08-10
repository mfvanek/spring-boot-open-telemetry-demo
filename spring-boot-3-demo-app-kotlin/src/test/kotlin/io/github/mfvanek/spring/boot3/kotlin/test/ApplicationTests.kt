package io.github.mfvanek.spring.boot3.kotlin.test

import io.github.mfvanek.spring.boot3.kotlin.test.support.JaegerInitializer
import io.github.mfvanek.spring.boot3.kotlin.test.support.TestBase
import io.micrometer.observation.ObservationRegistry
import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.otel.bridge.OtelTracer
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.dao.QueryTimeoutException
import java.util.*
import java.util.function.Consumer

class ApplicationTests : TestBase() {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun contextLoads() {
        assertThat(applicationContext.containsBean("otlpMeterRegistry"))
            .isFalse()

        assertThat(applicationContext.getBean(ObservationRegistry::class.java))
            .isNotNull
            .isInstanceOf(ObservationRegistry::class.java)

        assertThat(applicationContext.getBean(Tracer::class.java))
            .isNotNull
            .isInstanceOf(OtelTracer::class.java)
            .satisfies(
                Consumer { t: Tracer ->
                    assertThat(t.currentSpan())
                        .isNotEqualTo(Span.NOOP)
                }
            )

        assertThat(applicationContext.getBean("otlpGrpcSpanExporter"))
            .isNotNull
            .isInstanceOf(OtlpGrpcSpanExporter::class.java)
            .satisfies(
                Consumer { e ->
                    assertThat(e.toString())
                        .contains(
                            String.format(
                                Locale.ROOT,
                                """
                        OtlpGrpcSpanExporter{exporterName=otlp, type=span, endpoint=http://localhost:%d, endpointPath=/opentelemetry.proto.collector.trace.v1.TraceService/Export, timeoutNanos=5000000000, connectTimeoutNanos=2000000000, compressorEncoding=gzip, headers=Headers{User-Agent=OBFUSCATED}, retryPolicy=RetryPolicy{maxAttempts=2, initialBackoff=PT1S, maxBackoff=PT5S, backoffMultiplier=1.5, retryExceptionPredicate=null},
                                """.trimIndent(),
                                JaegerInitializer.getFirstMappedPort()
                            )
                        )
                }
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
            .isInstanceOf(QueryTimeoutException::class.java)
            .hasMessageContaining("ERROR: canceling statement due to user request")
    }

    @Test
    @DisplayName("Does not throw exception when query does not exceed timeout")
    fun exceptionNotThrownWithNotLongQuery() {
        assertThatNoException().isThrownBy { jdbcTemplate.execute("select pg_sleep(0.9);") }
    }
}
