plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    gradlePluginPortal()
}

dependencies {
    implementation(platform("io.github.mfvanek:internal-spring-boot-3-bom:0.1.1"))
    implementation("org.springframework.boot:spring-boot-gradle-plugin")
}
