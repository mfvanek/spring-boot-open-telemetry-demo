plugins {
    id("sb-ot-demo.java-conventions")
    id("sb-ot-demo.forbidden-apis")
    id("sb-ot-demo.docker")
    alias(libs.plugins.spring.boot.v3)
    id("io.freefair.lombok")
}

dependencies {
    implementation(platform(project(":common-internal-bom")))
    implementation(platform("org.springdoc:springdoc-openapi:2.6.0"))
    implementation(platform(libs.spring.boot.v3.dependencies))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.4"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    implementation("org.springframework.kafka:spring-kafka")

    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.postgresql:postgresql")
    implementation("com.zaxxer:HikariCP")
    implementation(project(":db-migrations"))
    implementation("org.liquibase:liquibase-core")
    implementation("com.github.blagerweij:liquibase-sessionlock")
    implementation("net.ttddyy.observation:datasource-micrometer-spring-boot:1.0.6")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation("io.github.mfvanek:pg-index-health-test-starter")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
}

springBoot {
    buildInfo()
}
