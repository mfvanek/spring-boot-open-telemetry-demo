plugins {
    id("com.bmuschko.docker-java-application")
}

docker {
    javaApplication {
        baseImage.set("eclipse-temurin:17.0.7_7-jre-focal")
        maintainer.set("Ivan Vakhrushev")
        images.set(listOf("${project.name}:${project.version}", "${project.name}:latest"))
    }
}
