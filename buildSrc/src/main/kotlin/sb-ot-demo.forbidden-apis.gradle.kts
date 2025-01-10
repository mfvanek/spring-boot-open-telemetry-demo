/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

import de.thetaphi.forbiddenapis.gradle.CheckForbiddenApis

plugins {
    id("java")
    id("de.thetaphi.forbiddenapis")
}

forbiddenApis {
    bundledSignatures = setOf("jdk-unsafe", "jdk-deprecated", "jdk-internal", "jdk-non-portable", "jdk-system-out", "jdk-reflection")
    signaturesFiles = files("${rootDir}/config/forbidden-apis/forbidden-apis.txt")
    ignoreFailures = false
}

tasks {
    test {
        dependsOn(withType<CheckForbiddenApis>())
    }
}
