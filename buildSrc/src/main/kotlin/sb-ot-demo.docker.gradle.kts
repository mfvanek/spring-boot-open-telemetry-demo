/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

plugins {
    id("com.bmuschko.docker-java-application")
}

docker {
    javaApplication {
        baseImage.set("eclipse-temurin:25.0.5_8-jre-noble")
        maintainer.set("Ivan Vakhrushev")
        images.set(listOf("${project.name}:${project.version}", "${project.name}:latest"))
    }
}
