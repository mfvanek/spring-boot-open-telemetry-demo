plugins {
    id("sb-ot-demo.java-conventions")
    id("sb-ot-demo.forbidden-apis")
    id("sb-ot-demo.docker")
    id("org.springframework.boot") version "2.7.18"
    id("io.freefair.lombok")
}

dependencies {
    implementation(platform(project(":common-internal-bom")))
    implementation(platform("org.springdoc:springdoc-openapi:1.7.0")) {
        because("version 1.8.0 brings incompatible logging library")
    }
    implementation(platform("org.springframework.boot:spring-boot-dependencies:2.7.18"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2021.0.9"))
    implementation(platform("org.springframework.cloud:spring-cloud-sleuth-otel-dependencies:1.1.4"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-ui")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth:3.1.11") {
        because("The version is higher than in BOM")
        exclude(group = "org.springframework.cloud", module = "spring-cloud-sleuth-brave")
    }
    implementation("org.springframework.cloud:spring-cloud-sleuth-otel-autoconfigure")

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.postgresql:postgresql")
    implementation("com.zaxxer:HikariCP")
    implementation(project(":db-migrations"))
    implementation("org.liquibase:liquibase-core")
    implementation("com.github.blagerweij:liquibase-sessionlock")
    implementation("net.ttddyy:datasource-proxy:1.9") {
        because("https://github.com/jdbc-observations/datasource-proxy/issues/111")
    }
    implementation("net.logstash.logback:logstash-logback-encoder:7.3")

    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation("io.github.mfvanek:pg-index-health-test-starter")
}

springBoot {
    buildInfo()
}
