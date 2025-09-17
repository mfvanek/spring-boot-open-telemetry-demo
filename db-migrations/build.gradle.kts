plugins {
    id("java-library")
    id("sb-ot-demo.java-conventions")
    id("io.freefair.lombok")
}

dependencies {
    implementation(platform(project(":common-internal-bom")))
    implementation(platform(libs.spring.boot.v3.dependencies))

    implementation("io.micrometer:micrometer-tracing")
    implementation("org.apache.kafka:kafka-clients")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-jdbc")
}
