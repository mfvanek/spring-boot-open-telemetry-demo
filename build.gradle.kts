import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("base")
    id("io.github.ben-manes.versions") version "0.67.0"
}

description = "Experiments with Java"

allprojects {
    group = "io.github.mfvanek"
    version = "0.6.0"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

tasks {
    wrapper {
        gradleVersion = "9.8.2"
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    checkForGradleUpdate = true
    gradleReleaseChannel = "current"
    checkConstraints = true
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
