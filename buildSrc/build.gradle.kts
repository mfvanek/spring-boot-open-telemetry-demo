plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.1.0")
    implementation("io.freefair.gradle:lombok-plugin:8.12")
    implementation("com.bmuschko:gradle-docker-plugin:9.4.0")
    implementation("gradle.plugin.com.google.gradle:osdetector-gradle-plugin:1.7.3")
    implementation("de.thetaphi:forbiddenapis:3.8")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.1.0")
    implementation("org.gradle:test-retry-gradle-plugin:1.6.1")
}
