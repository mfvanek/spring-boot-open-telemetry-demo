plugins {
    id("sb-ot-demo.java-conventions")
    id("sb-ot-demo.docker")
    id("org.springframework.boot") version "3.2.2"
    id("io.freefair.lombok")
}

dependencies {
    implementation(platform(project(":common-internal-bom")))
    implementation(platform("org.springdoc:springdoc-openapi:2.3.0"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    implementation("org.springframework.kafka:spring-kafka")

    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

springBoot {
    buildInfo()
}
