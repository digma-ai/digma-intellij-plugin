@Suppress(
    //see: https://youtrack.jetbrains.com/issue/KTIJ-19369
    "DSL_SCOPE_VIOLATION"
)
plugins {
    id("common-java-library")
    id("common-kotlin")
}



dependencies{
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.6.4")
    //add kotlin stdlib version compatible with the intellij platform we're building for.
    //compileOnly because we don't need to package it in the plugin zip.
    compileOnly(libs.kotlin.stdlib.jdk8)
}
