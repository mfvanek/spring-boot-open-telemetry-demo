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

private val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    libs.findLibrary("detekt-formatting").ifPresent {
        detektPlugins(it)
    }
    libs.findLibrary("detekt-libraries").ifPresent {
        detektPlugins(it)
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_21
    }
}

detekt {
    toolVersion = libs.findVersion("detekt").get().requiredVersion
    config.setFrom(file("${rootDir}/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
}
