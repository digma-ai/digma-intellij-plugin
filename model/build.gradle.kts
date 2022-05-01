@Suppress(
    //see: https://youtrack.jetbrains.com/issue/KTIJ-19369
    "DSL_SCOPE_VIOLATION"
)
plugins {
    id("common-java-library")
    id("common-kotlin")
}



dependencies{
    //this project needs jackson2 not the resteasy provider,
    // but adding resteasy.jackson2.provider makes sure we use the same dependencies as
    // in analytics-provider module.
    implementation(libs.resteasy.jackson2.provider)
    //add kotlin stdlib version compatible with the intellij platform we're building for.
    //compileOnly because we don't need to package it in the plugin zip.
    compileOnly(libs.kotlin.stdlib.jdk8)
}
