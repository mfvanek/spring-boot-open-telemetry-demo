plugins {
    id("java-library")
    id("sb-ot-demo.java-conventions")
}

description = "Code shared between the demo applications"

dependencies {
    implementation(platform(project(":common-internal-bom")))
    implementation(platform(libs.spring.boot.v4.dependencies))
}
