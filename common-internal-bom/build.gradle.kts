/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

plugins {
    id("java-platform")
}

description = "BOM for internal usage"

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("org.assertj:assertj-bom:3.27.3"))
    api(platform("org.testcontainers:testcontainers-bom:1.20.6"))
    api(platform("org.junit:junit-bom:5.12.1"))
    api(platform("io.github.mfvanek:pg-index-health-bom:0.14.4"))

    constraints {
        api("org.liquibase:liquibase-core:4.31.1")
        api("com.github.blagerweij:liquibase-sessionlock:1.6.9")
        api("org.awaitility:awaitility:4.3.0")
        api("com.zaxxer:HikariCP:6.3.0")
        api("org.postgresql:postgresql:42.7.5")
    }
}
