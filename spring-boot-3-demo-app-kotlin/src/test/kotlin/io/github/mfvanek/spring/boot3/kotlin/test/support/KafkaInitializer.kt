package io.github.mfvanek.spring.boot3.kotlin.test.support

import org.apache.kafka.common.security.plain.PlainLoginModule
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.Locale

class KafkaInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        KAFKA_CONTAINER.start()
        TestPropertyValues.of(
            "spring.kafka.bootstrap-servers=${KAFKA_CONTAINER.bootstrapServers}",
            "demo.kafka.opentelemetry.username=$KAFKA_USER_NAME",
            "demo.kafka.opentelemetry.password=$KAFKA_USER_PASSWORD"
        ).applyTo(applicationContext.environment)
    }
    companion object {
        @JvmStatic
        private val KAFKA_USER_NAME = "sb-ot-demo-user"

        @JvmStatic
        private val KAFKA_USER_PASSWORD = "pwdForSbOtDemoApp"

        @JvmStatic
        private val IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka:7.7.3").asCompatibleSubstituteFor("apache/kafka")

        @JvmStatic
        private val KAFKA_CONTAINER = KafkaContainer(IMAGE_NAME)
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SASL_PLAINTEXT,BROKER:PLAINTEXT")
            .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
            .withEnv("KAFKA_SASL_JAAS_CONFIG", plainJaas())
            .withEnv(
                "KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG",
                plainJaas(mapOf(KAFKA_USER_NAME to KAFKA_USER_PASSWORD))
            )

        @JvmStatic
        fun plainJaas(): String {
            return plainJaas(mapOf())
        }

        @JvmStatic
        private fun plainJaas(additionalUsers: Map<String, String>): String {
            val users = additionalUsers.entries.joinToString(separator = " ") { String.format(Locale.ROOT, "user_%s=\"%s\"", it.key, it.value) }
            val builder = StringBuilder().append(PlainLoginModule::class.java.name).append(
                String.format(
                    Locale.ROOT,
                    " required username=\"%s\" password=\"%s\"",
                    KAFKA_USER_NAME,
                    KAFKA_USER_PASSWORD
                )
            )
            if (users.isNotBlank()) {
                builder.append(' ')
                    .append(users)
            }
            return builder.append(';')
                .toString()
        }

        @JvmStatic
        fun getBootstrapSevers(): String = KAFKA_CONTAINER.bootstrapServers
    }
}
