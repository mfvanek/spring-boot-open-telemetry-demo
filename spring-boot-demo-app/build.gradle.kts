plugins {
    id("sb-ot-demo.java-conventions")
    id("sb-ot-demo.forbidden-apis")
    id("sb-ot-demo.docker")
    alias(libs.plugins.spring.boot.v4)
    id("io.freefair.lombok")
}

dependencies {
    implementation(platform(project(":common-internal-bom")))
    implementation(platform(libs.springdoc.openapi))
    implementation(platform(libs.spring.boot.v4.dependencies))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    implementation("org.springframework.boot:spring-boot-starter-kafka")

    implementation("org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.postgresql:postgresql")
    implementation("com.zaxxer:HikariCP")
    implementation(project(":core"))
    implementation(project(":db-migrations"))
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("com.github.blagerweij:liquibase-sessionlock")
    implementation(libs.datasource.micrometer)
    implementation(libs.logstash)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-micrometer-tracing-test")
    testImplementation("org.springframework.boot:spring-boot-micrometer-metrics-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-webtestclient")
    testImplementation(project(":test-initializers"))
    testImplementation("org.springframework.boot:spring-boot-starter-kafka-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation("io.github.mfvanek:pg-index-health-test-starter")
    testImplementation(libs.wiremock.spring.boot)
}

tasks {
    jacocoTestCoverageVerification {
        dependsOn(jacocoTestReport)
        violationRules {
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.66".toBigDecimal()
                }
            }
        }
    }
}

springBoot {
    buildInfo()
}
