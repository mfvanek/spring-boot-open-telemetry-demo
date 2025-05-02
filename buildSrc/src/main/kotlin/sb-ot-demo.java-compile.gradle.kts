/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

import gradle.kotlin.dsl.accessors._82e33fab8c067a1df5c2e280e32692b4.check
import gradle.kotlin.dsl.accessors._82e33fab8c067a1df5c2e280e32692b4.jacocoTestCoverageVerification
import gradle.kotlin.dsl.accessors._82e33fab8c067a1df5c2e280e32692b4.jacocoTestReport
import gradle.kotlin.dsl.accessors._82e33fab8c067a1df5c2e280e32692b4.java
import gradle.kotlin.dsl.accessors._82e33fab8c067a1df5c2e280e32692b4.osdetector
import gradle.kotlin.dsl.accessors._82e33fab8c067a1df5c2e280e32692b4.test
import gradle.kotlin.dsl.accessors._82e33fab8c067a1df5c2e280e32692b4.testImplementation
import gradle.kotlin.dsl.accessors._82e33fab8c067a1df5c2e280e32692b4.testRuntimeOnly

plugins {
    id("java")
    id("jacoco")
    id("com.google.osdetector")
    id("org.gradle.test-retry")
}
dependencies {
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // https://github.com/netty/netty/issues/11020
    if (osdetector.arch == "aarch_64") {
        testImplementation("io.netty:netty-all:4.1.104.Final")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("--should-stop=ifError=FLOW")
    }

    test {
        useJUnitPlatform()

        finalizedBy(jacocoTestReport, jacocoTestCoverageVerification)
        maxParallelForks = 1

        retry {
            maxRetries.set(2)
            maxFailures.set(5)
            failOnPassedAfterRetry.set(false)
        }
    }

    jacocoTestCoverageVerification {
        dependsOn(jacocoTestReport)
        violationRules {
            rule {
                limit {
                    counter = "CLASS"
                    value = "MISSEDCOUNT"
                    maximum = "0.0".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "METHOD"
                    value = "MISSEDCOUNT"
                    maximum = "2.0".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "LINE"
                    value = "MISSEDCOUNT"
                    maximum = "7.0".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                    minimum = "0.93".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.66".toBigDecimal()
                }
            }
        }
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    check {
        dependsOn(jacocoTestCoverageVerification)
    }
}
