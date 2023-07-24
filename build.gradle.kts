import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("java")
    id("org.springframework.boot") version "3.1.2"
    id("io.spring.dependency-management") version "1.1.2"
    id("com.bmuschko.docker-java-application") version "9.3.1"
    id("io.freefair.lombok") version "8.1.0"
    id("com.google.osdetector") version "1.7.3"
    id("com.github.ben-manes.versions") version "0.47.0"
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
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // https://github.com/netty/netty/issues/11020
    if (osdetector.arch == "aarch_64") {
        testImplementation("io.netty:netty-all:4.1.95.Final")
    }

    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.18.3"))
    testImplementation("org.testcontainers:junit-jupiter")
}

dependencyManagement {
    imports {
        mavenBom("io.micrometer:micrometer-bom:1.11.2")
        mavenBom("io.micrometer:micrometer-tracing-bom:1.1.3")
        mavenBom("io.opentelemetry:opentelemetry-bom:1.28.0")
        mavenBom("org.junit:junit-bom:5.10.0")
    }
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

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    checkForGradleUpdate = true
    gradleReleaseChannel = "current"
    checkConstraints = true
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
