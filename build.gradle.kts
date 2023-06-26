plugins {
    id("java")
    id("org.springframework.boot") version "3.1.0"
    id("io.spring.dependency-management") version "1.1.0"
    id("com.bmuschko.docker-java-application") version "9.3.1"
    id("io.freefair.lombok") version "8.0.1"
    id("com.google.osdetector") version "1.7.3"
}

group = "io.github.mfvanek"
version = "0.0.7"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(enforcedPlatform("io.micrometer:micrometer-bom:1.11.1"))
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

    implementation(enforcedPlatform("io.micrometer:micrometer-tracing-bom:1.1.2"))
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation(enforcedPlatform("io.opentelemetry:opentelemetry-bom:1.26.0"))
    implementation("io.opentelemetry:opentelemetry-exporter-jaeger")

    // https://github.com/netty/netty/issues/11020
    if (osdetector.arch == "aarch_64") {
        testImplementation("io.netty:netty-all:4.1.94.Final")
    }

    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation(enforcedPlatform("org.junit:junit-bom:5.9.3"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation(enforcedPlatform("org.testcontainers:testcontainers-bom:1.18.3"))
    testImplementation("org.testcontainers:junit-jupiter")
}

tasks {
    test {
        useJUnitPlatform()
    }
}

docker {
    javaApplication {
        baseImage.set("eclipse-temurin:17.0.7_7-jre-focal")
        maintainer.set("Ivan Vakhrushev")
        images.set(listOf("${project.name}:${project.version}", "${project.name}:latest"))
    }
}

springBoot {
    buildInfo()
}
