plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.1.0")
    implementation("io.freefair.gradle:lombok-plugin:8.11")
    implementation("com.bmuschko:gradle-docker-plugin:9.4.0")
    implementation("gradle.plugin.com.google.gradle:osdetector-gradle-plugin:1.7.3")
}
