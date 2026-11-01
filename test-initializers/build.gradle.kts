plugins {
    id("java-library")
    id("sb-ot-demo.java-conventions")
}

description = "Testcontainers-based ApplicationContextInitializers shared between the demo applications"

dependencies {
    implementation(platform(project(":common-internal-bom")))
    implementation(platform(libs.spring.boot.v4.dependencies))

    api("org.springframework:spring-context")
    implementation("org.apache.kafka:kafka-clients")
    implementation("org.springframework.boot:spring-boot-test")
    implementation("org.testcontainers:testcontainers")
    implementation("org.testcontainers:testcontainers-kafka")
    implementation("org.testcontainers:testcontainers-postgresql")
}
