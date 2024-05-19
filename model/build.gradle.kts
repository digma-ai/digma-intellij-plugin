plugins {
    id("common-java-library")
    id("common-kotlin")
}

dependencies {

    api(libs.prettytime)

    //this project needs jackson not retrofit,
    // but adding retrofit.jackson makes sure we use the same dependencies as
    // in analytics-provider module.
    compileOnly(libs.retrofit.jackson)
    //add kotlin stdlib version compatible with the intellij platform we're building for.
    //compileOnly because we don't need to package it in the plugin zip.
    compileOnly(libs.kotlin.stdlib.jdk8)

}