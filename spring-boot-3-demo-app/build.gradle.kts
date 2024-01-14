plugins {
    id("sb-ot-demo.java-conventions")
    id("sb-ot-demo.docker")
    id("org.springframework.boot") version "3.2.1"
    id("io.freefair.lombok")
}

dependencies {
    implementation(platform("org.springdoc:springdoc-openapi:2.3.0"))
    implementation(platform("org.assertj:assertj-bom:3.25.1"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.1"))
    implementation(platform("org.testcontainers:testcontainers-bom:1.19.3"))
    implementation(platform("org.junit:junit-bom:5.10.1"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.testcontainers:junit-jupiter")
}

springBoot {
    buildInfo()
}
