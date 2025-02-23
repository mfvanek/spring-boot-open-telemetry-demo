rootProject.name = "spring-boot-open-telemetry-demo"
include("spring-boot-3-demo-app")
include("common-internal-bom")
include("spring-boot-2-demo-app")
include("db-migrations")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val springBoot3Version = version("spring-boot-v3", "3.3.9")
            plugin("spring-boot-v3", "org.springframework.boot")
                .versionRef(springBoot3Version)
            library("spring-boot-v3-dependencies", "org.springframework.boot", "spring-boot-dependencies")
                .versionRef(springBoot3Version)
        }
    }
}
