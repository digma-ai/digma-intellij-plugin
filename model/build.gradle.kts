@Suppress(
    //see: https://youtrack.jetbrains.com/issue/KTIJ-19369
    "DSL_SCOPE_VIOLATION"
)
plugins {
    id("common-java-library")
    id("common-kotlin")
}

dependencies {
    //this project needs jackson not retrofit,
    // but adding retrofit.jackson makes sure we use the same dependencies as
    // in analytics-provider module.
    compileOnly(libs.retrofit.jackson)
    //add kotlin stdlib version compatible with the intellij platform we're building for.
    //compileOnly because we don't need to package it in the plugin zip.
    compileOnly(libs.kotlin.stdlib.jdk8)

    implementation(project(":common"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
