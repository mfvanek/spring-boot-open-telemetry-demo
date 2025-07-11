plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.2.0")
    implementation("io.freefair.gradle:lombok-plugin:8.14")
    implementation("com.bmuschko:gradle-docker-plugin:9.4.0")
    implementation("gradle.plugin.com.google.gradle:osdetector-gradle-plugin:1.7.3")
    implementation("de.thetaphi:forbiddenapis:3.9")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.1.13")
    implementation("org.gradle:test-retry-gradle-plugin:1.6.2")
    val kotlinVersion = "2.0.21"
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
    implementation(libs.detekt)
}
