import common.rider.rdLibDirectory

plugins {
    kotlin("jvm") version "1.6.10"
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
    implementation(kotlin("stdlib"))
    implementation("com.jetbrains:rd-gen")
    implementation("com.jetbrains:rider-model")
}