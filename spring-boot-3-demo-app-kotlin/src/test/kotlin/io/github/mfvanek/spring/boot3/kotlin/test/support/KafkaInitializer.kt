package io.github.mfvanek.spring.boot3.kotlin.test.support

import org.apache.kafka.common.security.plain.PlainLoginModule
import org.springframework.boot.test.util.TestPropertyValues.of
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

private const val KAFKA_USER_NAME = "sb-ot-demo-user"
private const val KAFKA_USER_PASSWORD = "pwdForSbOtDemoApp"

internal class KafkaInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        KAFKA_CONTAINER.start()
        of(
            "spring.kafka.bootstrap-servers=${KAFKA_CONTAINER.bootstrapServers}",
            "demo.kafka.opentelemetry.username=$KAFKA_USER_NAME",
            "demo.kafka.opentelemetry.password=$KAFKA_USER_PASSWORD"
        ).applyTo(applicationContext.environment)
    }

    companion object {
        private val IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka:7.7.1")
        private val KAFKA_CONTAINER = KafkaContainer(IMAGE_NAME)
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SASL_PLAINTEXT,BROKER:PLAINTEXT")
            .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
            .withEnv("KAFKA_SASL_JAAS_CONFIG", plainJaas())
            .withEnv(
                "KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG",
                plainJaas(mapOf(KAFKA_USER_NAME to KAFKA_USER_PASSWORD))
            )

        internal fun plainJaas(additionalUsers: Map<String, String> = mapOf()): String =
            additionalUsers.entries
                .joinToString(" ") { (key, value) -> "user_$key=\"$value\"" }
                .let { "${PlainLoginModule::class.java.name} required username=\"$KAFKA_USER_NAME\" password=\"$KAFKA_USER_PASSWORD\" $it;" }

        internal fun getBootstrapSevers(): String = KAFKA_CONTAINER.bootstrapServers
    }
}
