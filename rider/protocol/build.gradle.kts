import common.rider.rdLibDirectory
@Suppress(
    //see: https://youtrack.jetbrains.com/issue/KTIJ-19369
    "DSL_SCOPE_VIOLATION"
)
plugins {
    id("common-kotlin")
}

group = "org.digma.plugins.rider.protocol"
version = "0.0.2"

repositories {
    mavenCentral()
    flatDir{
        dirs(rdLibDirectory(project(":rider")).absolutePath)
    }
}

dependencies {
    compileOnly(libs.kotlin.stdlib.jdk8)
    implementation("com.jetbrains:rd-gen")
    implementation("com.jetbrains:rider-model")
}