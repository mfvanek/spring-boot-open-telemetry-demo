plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.3.0")
    implementation("io.freefair.gradle:lombok-plugin:9.2.0")
    implementation("com.bmuschko:gradle-docker-plugin:9.4.0")
    implementation("gradle.plugin.com.google.gradle:osdetector-gradle-plugin:1.7.3")
    implementation("de.thetaphi:forbiddenapis:3.10")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.4.4")
    implementation("org.gradle:test-retry-gradle-plugin:1.6.4")
    val kotlinVersion = "2.0.21"
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
    implementation(libs.detekt)
}
