package io.github.mfvanek.spring.boot3.kotlin.test.support

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class PostgresInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(context: ConfigurableApplicationContext) {
        CONTAINER
            .withNetwork(NETWORK)
            .withUsername("otel_demo_user")
            .withPassword("otel_demo_password")
            .withUrlParam("prepareThreshold", "0")
            .waitingFor(Wait.forListeningPort())
            .start()

        TestPropertyValues.of(
            "spring.datasource.url=${CONTAINER.jdbcUrl}",
            "spring.datasource.username=${CONTAINER.username}",
            "spring.datasource.password=${CONTAINER.password}"
        ).applyTo(context.environment)
    }

    companion object {
        @JvmStatic
        private val IMAGE = DockerImageName.parse("postgres:17.4")

        @JvmStatic
        private val NETWORK = Network.newNetwork()

        @JvmStatic
        private val CONTAINER = PostgreSQLContainer(IMAGE)
    }
}
