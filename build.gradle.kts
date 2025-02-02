import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("base")
    id("com.github.ben-manes.versions") version "0.52.0"
}

description = "Experiments with Java"

allprojects {
    group = "io.github.mfvanek"
    version = "0.3.1"

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

tasks {
    wrapper {
        gradleVersion = "8.12.1"
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
