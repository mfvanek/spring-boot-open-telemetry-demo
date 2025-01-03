plugins {
    id("java-platform")
}

description = "BOM for internal usage"

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("org.assertj:assertj-bom:3.27.0"))
    api(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    api(platform("org.junit:junit-bom:5.11.4"))
    api(platform("io.github.mfvanek:pg-index-health-bom:0.14.4"))

    constraints {
        api("org.liquibase:liquibase-core:4.30.0")
        api("com.github.blagerweij:liquibase-sessionlock:1.6.9")
        api("org.awaitility:awaitility:4.2.2")
    }
}
