/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("jacoco")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("io.gitlab.arturbosch.detekt")
    id("sb-ot-demo.forbidden-apis")
    id("sb-ot-demo.java-compile")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    //implementation("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
    implementation("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.6")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_17
    }
}

detekt {
    toolVersion = "1.23.6"
    config.setFrom(file("${rootDir}/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

tasks {
    withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("--should-stop=ifError=FLOW")
    }

    test {
        testLogging.showStandardStreams = false // set to true for debug purposes
    }
}
