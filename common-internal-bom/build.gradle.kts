plugins {
    id("java-platform")
}

description = "BOM for internal usage"

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("org.assertj:assertj-bom:3.26.3"))
    api(platform("org.testcontainers:testcontainers-bom:1.20.0"))
    api(platform("org.junit:junit-bom:5.10.3"))
}
