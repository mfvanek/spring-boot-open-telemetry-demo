plugins {
    id("java-platform")
}

description = "BOM for internal usage"

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("org.assertj:assertj-bom:3.25.2"))
    api(platform("org.testcontainers:testcontainers-bom:1.19.4"))
    api(platform("org.junit:junit-bom:5.10.2"))
}
