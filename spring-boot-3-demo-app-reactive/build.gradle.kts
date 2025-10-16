plugins {
    id("sb-ot-demo.java-conventions")
    id("sb-ot-demo.forbidden-apis")
    id("sb-ot-demo.docker")
    alias(libs.plugins.spring.boot.v3)
    id("io.freefair.lombok")
}

dependencies {
    implementation(platform(project(":common-internal-bom")))
    implementation(platform(libs.springdoc.openapi))
    implementation(platform(libs.spring.boot.v3.dependencies))
    implementation(platform(libs.spring.cloud))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.projectreactor:reactor-tools")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui")

    implementation("org.springframework.kafka:spring-kafka")

    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.postgresql:postgresql")
    implementation("com.zaxxer:HikariCP")
    implementation(project(":db-migrations"))
    implementation("org.liquibase:liquibase-core")
    implementation("com.github.blagerweij:liquibase-sessionlock")
    implementation(libs.datasource.micrometer)
    implementation(libs.logstash)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation("io.github.mfvanek:pg-index-health-test-starter")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("io.projectreactor.tools:blockhound:1.0.15.RELEASE")
}

tasks {
    withType<Test>().all {
        jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
    }

    jacocoTestCoverageVerification {
        dependsOn(jacocoTestReport)
        violationRules {
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.50".toBigDecimal()
                }
            }
        }
    }
}

val coverageExcludeList = listOf("**/*Application.class")
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
