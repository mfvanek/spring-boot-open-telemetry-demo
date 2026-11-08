plugins {
    id("java-library")
    id("sb-ot-demo.java-conventions")
    id("sb-ot-demo.forbidden-apis")
}

description = "Spring Boot starter that adds the current traceId to HTTP responses (servlet and reactive stacks)"

dependencies {
    implementation(platform(project(":common-internal-bom")))
    implementation(platform(libs.spring.boot.v4.dependencies))

    api("org.springframework.boot:spring-boot-autoconfigure")
    api("io.micrometer:micrometer-tracing")
    implementation("org.slf4j:slf4j-api")

    // Web stacks are optional: the application decides which one is used
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.springframework:spring-web")
    compileOnly("io.projectreactor:reactor-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-webflux") // required to detect a reactive web application
    testImplementation("io.projectreactor:reactor-core")
    testImplementation("org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry")
    testImplementation("io.micrometer:micrometer-tracing-bridge-otel")
}
