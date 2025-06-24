plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "spring-boot-open-telemetry-demo"

include("spring-boot-3-demo-app")
include("common-internal-bom")
include("spring-boot-2-demo-app")
include("db-migrations")
include("spring-boot-3-demo-app-kotlin")
include("spring-boot-3-demo-app-reactive")
