import common.properties


/*
apply this plugin to projects that need the kotlin plugin.
note that this plugin is applied to various projects, the projects configuration of the plugin is not always
the same. so pay attention when adding configurations here as it will impact many projects.

usually the projects opt out of kotlin stdlib dependency by using kotlin.stdlib.default.dependency=false in gradle.properties.
if a project needs kotlin stdlib for compilation then a compileOnly dependency needs to be added.
for example compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.10")
the stdlib version must be compatiable with the intellij platform we're building for,
see here: https://plugins.jetbrains.com/docs/intellij/kotlin.html#kotlin-standard-library

 */

plugins {
    `java`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain {
            (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(properties("javaVersion", project)))
        //to build with amazon corretto download it in the gradlew script. see resharper-unity
        //vendor.set(JvmVendorSpec.AMAZON)
    }
}