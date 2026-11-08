
plugins {
    id("sb-ot-demo.kotlin-conventions")
    id("sb-ot-demo.forbidden-apis")
    id("sb-ot-demo.docker")
    alias(libs.plugins.spring.boot.v4)
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
    implementation(project(":trace-id-spring-boot-starter"))
    implementation(project(":db-migrations"))
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("com.github.blagerweij:liquibase-sessionlock")
    implementation(libs.datasource.micrometer)
    implementation(libs.logstash)
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.14")

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
                    counter = "CLASS"
                    value = "MISSEDCOUNT"
                    maximum = "0.0".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "METHOD"
                    value = "MISSEDCOUNT"
                    maximum = "6.0".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "LINE"
                    value = "MISSEDCOUNT"
                    maximum = "0.0".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                    minimum = "0.90".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.57".toBigDecimal()
                }
            }
        }
    }
}

val coverageExcludeList = listOf("**/*ApplicationKt.class")
listOf(JacocoCoverageVerification::class, JacocoReport::class).forEach { taskType ->
    tasks.withType(taskType) {
        afterEvaluate {
            classDirectories.setFrom(
                files(
                    classDirectories.files.map { file ->
                        fileTree(file).apply {
                            exclude(coverageExcludeList)
                        }
                    }
                )
            )
        }
    }
}

springBoot {
    buildInfo()
}
