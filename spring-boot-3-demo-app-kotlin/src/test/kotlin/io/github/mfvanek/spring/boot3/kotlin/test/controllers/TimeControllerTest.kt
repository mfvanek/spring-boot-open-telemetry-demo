package io.github.mfvanek.spring.boot3.kotlin.test.controllers

import io.github.mfvanek.spring.boot3.kotlin.test.filters.TraceIdInResponseServletFilter.Companion.TRACE_ID_HEADER_NAME
import io.github.mfvanek.spring.boot3.kotlin.test.service.dto.toParsedDateTime
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.testcontainers.shaded.org.awaitility.Awaitility
import io.github.mfvanek.spring.boot3.kotlin.test.support.KafkaInitializer
import io.github.mfvanek.spring.boot3.kotlin.test.support.TestBase
import net.bytebuddy.utility.dispatcher.JavaDispatcher.Container
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@ExtendWith(OutputCaptureExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimeControllerTest : TestBase() {
    private lateinit var container: KafkaMessageListenerContainer<UUID, String>
    private val consumerRecords = LinkedBlockingQueue<ConsumerRecord<UUID, String>>()

    @Autowired
    private lateinit var kafkaProperties: KafkaProperties

    @BeforeAll
    fun setUpKafkaConsumer() {
        @Container
        @JvmStatic
        container = setUpKafkaConsumer(kafkaProperties, consumerRecords)
    }

    @AfterAll
    fun tearDownKafkaConsumer() {
        container.stop()
    }

    @BeforeEach
    fun cleanUpDatabase() {
        jdbcTemplate.execute("truncate table otel_demo.storage")
    }

    @Order(1)
    @Test
    fun spanShouldBeReportedInLogs(output: CapturedOutput) {
        stubOkResponse((LocalDateTime.now(clock).minusDays(1)).toParsedDateTime());

        val result = webTestClient.get()
            .uri { uriBuilder -> uriBuilder.path("current-time").build() }
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(LocalDateTime::class.java)
            .returnResult()
        val traceId = result.responseHeaders.getFirst(TRACE_ID_HEADER_NAME);
        assertThat(traceId).isNotBlank()
        assertThat(result.responseBody)
            .isBefore(LocalDateTime.now(clock))
        assertThat(output.all)
            .contains("Called method getNow. TraceId = $traceId")
            .contains("Awaiting acknowledgement from Kafka");

        val received = consumerRecords.poll(10, TimeUnit.SECONDS)
        assertThat(received).isNotNull()
        assertThatTraceIdPresentInKafkaHeaders(received!!, traceId!!)

        awaitStoringIntoDatabase();

        assertThat(output.all)
            .contains("Received record: " + received.value() + " with traceId " + traceId)
            .contains("\"tenant.name\":\"ru-a1-private\"");
        val messageFromDb = namedParameterJdbcTemplate.queryForObject(
            "select message from otel_demo.storage where trace_id = :traceId",
            mapOf("traceId" to traceId), String::class.java
        )
        assertThat(messageFromDb).isEqualTo(received.value())
    }

    @Order(2)
    @Test
    fun spanAndMdcShouldBeReportedWhenRetry(output: CapturedOutput) {
        val zoneName = stubErrorResponse()

        val result = webTestClient.get().uri { uriBuilder -> uriBuilder.path("current-time").build() }
            .header("traceparent", "00-38c19768104ab8ae64fabbeed65bbbdf-4cac1747d4e1ee10-01")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists(TRACE_ID_HEADER_NAME)
            .expectBody(LocalDateTime::class.java)
            .returnResult()
        val traceId = result.responseHeaders.getFirst(TRACE_ID_HEADER_NAME);
        assertThat(traceId)
            .isEqualTo("38c19768104ab8ae64fabbeed65bbbdf");

        assertThat(output.all)
            .containsPattern(
                String.format(
                    Locale.ROOT,
                    ".*\"message\":\"Retrying request to '${Locale.ROOT}', attempt 1/1 due to error:\"," +
                        "\"logger\":\"io\\.github\\.mfvanek\\.spring\\.boot3\\.test\\.service\\.PublicApiService\"," +
                        "\"thread\":\"[^\"]+\",\"level\":\"INFO\",\"stack_trace\":\".+?\"," +
                        "\"traceId\":\"38c19768104ab8ae64fabbeed65bbbdf\",\"spanId\":\"[a-f0-9]+\",\"instance_timezone\":\"${Locale.ROOT}\",\"applicationName\":\"spring-boot-3-demo-app\"\\}%n",
                    zoneName
                )
            )
            .containsPattern(
                String.format(
                    Locale.ROOT,
                    ".*\"message\":\"Request to '/%s' failed after 2 attempts.\",\"logger\":\"io\\.github\\.mfvanek\\.spring\\.boot3\\.test\\.service\\.PublicApiService\"," +
                        "\"thread\":\"[^\"]+\",\"level\":\"ERROR\"," +
                        "\"traceId\":\"38c19768104ab8ae64fabbeed65bbbdf\",\"spanId\":\"[a-f0-9]+\",\"applicationName\":\"spring-boot-3-demo-app\"}%n",
                    zoneName
                )
            )
    }

    private fun countRecordsInTable(): Long {
        val queryResult = jdbcTemplate.queryForObject("select count(*) from otel_demo.storage", Long::class.java)
        return queryResult ?: 0L
    }

    private fun assertThatTraceIdPresentInKafkaHeaders(received: ConsumerRecord<UUID, String>, expectedTraceId: String) {
        assertThat(received.value()).startsWith("Current time = ");
        val headers = received.headers().toArray();
        val headerNames = headers.map { it.key() }
        assertThat(headerNames)
            .hasSize(2)
            .containsExactlyInAnyOrder("traceparent", "b3");
        val headerValues = headers.map { it.key() }
            .map { v -> String(v.toByteArray(), StandardCharsets.UTF_8) }
        assertThat(headerValues)
            .hasSameSizeAs(headerNames)
            .allSatisfy { h -> assertThat(h).contains(expectedTraceId) }
    }

    private fun awaitStoringIntoDatabase() {
        Awaitility
            .await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500L))
            .until { countRecordsInTable() >= 1L }
    }
}

fun setUpKafkaConsumer(kafkaProperties: KafkaProperties, consumerRecords: BlockingQueue<ConsumerRecord<UUID, String>>): KafkaMessageListenerContainer<UUID, String> {
    val containerProperties = ContainerProperties(kafkaProperties.template.defaultTopic)
    val consumerProperties = KafkaTestUtils.consumerProps(KafkaInitializer.getBootstrapSevers(), "test-group", "false");
    consumerProperties[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_PLAINTEXT"
    consumerProperties[SaslConfigs.SASL_MECHANISM] = "PLAIN"
    consumerProperties[SaslConfigs.SASL_JAAS_CONFIG] = KafkaInitializer.plainJaas()
    consumerProperties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = UUIDDeserializer::class.java
    val consumer = DefaultKafkaConsumerFactory<UUID, String>(consumerProperties)
    val container = KafkaMessageListenerContainer(consumer, containerProperties)
    container.setupMessageListener(MessageListener { consumerRecords.add(it) })
    container.start()
    ContainerTestUtils.waitForAssignment(container, 1)
    return container
}
