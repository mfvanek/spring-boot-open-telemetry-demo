rootProject.name = "spring-boot-open-telemetry-demo"
include("spring-boot-3-demo-app")
include("common-internal-bom")
include("spring-boot-2-demo-app")
include("db-migrations")
include("spring-boot-3-demo-app-kotlin")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val springBoot3Version = version("spring-boot-v3", "3.4.4")
            plugin("spring-boot-v3", "org.springframework.boot")
                .versionRef(springBoot3Version)
            library("spring-boot-v3-dependencies", "org.springframework.boot", "spring-boot-dependencies")
                .versionRef(springBoot3Version)
            library("springdoc-openapi", "org.springdoc:springdoc-openapi:2.8.6")
            library("spring-cloud", "org.springframework.cloud:spring-cloud-dependencies:2024.0.1")
            library("datasource-micrometer", "net.ttddyy.observation:datasource-micrometer-spring-boot:1.1.0")
            library("logstash", "net.logstash.logback:logstash-logback-encoder:8.0")
        }
    }
}
