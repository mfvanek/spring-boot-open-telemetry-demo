plugins {
    id("java-platform")
}

description = "BOM for internal usage"

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("org.assertj:assertj-bom:3.26.3"))
    api(platform("org.testcontainers:testcontainers-bom:1.20.1"))
    api(platform("org.junit:junit-bom:5.11.0"))
    api(platform("io.github.mfvanek:pg-index-health-bom:0.13.0"))

    constraints {
        api("org.liquibase:liquibase-core:4.29.2")
        api("com.github.blagerweij:liquibase-sessionlock:1.6.9")
        api("org.awaitility:awaitility:4.2.2")
    }
}
