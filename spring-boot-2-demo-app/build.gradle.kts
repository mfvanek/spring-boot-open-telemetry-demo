plugins {
    id("sb-ot-demo.java-conventions")
    id("sb-ot-demo.docker")
    id("org.springframework.boot") version "2.7.18"
    id("io.freefair.lombok")
}

dependencies {
    implementation(platform(project(":common-internal-bom")))
    implementation(platform("org.springdoc:springdoc-openapi:1.7.0"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:2.7.18"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2021.0.9"))
    implementation(platform("org.springframework.cloud:spring-cloud-sleuth-otel-dependencies:1.1.4"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-ui")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth") {
        exclude(group = "org.springframework.cloud", module = "spring-cloud-sleuth-brave")
    }
    implementation("org.springframework.cloud:spring-cloud-sleuth-otel-autoconfigure")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.testcontainers:junit-jupiter")
}

springBoot {
    buildInfo()
}
