import common.currentProfile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


/*
apply this plugin to projects that need the kotlin plugin.
note that this plugin is applied to various projects, the projects configuration of the plugin is not always
the same. so pay attention when adding configurations here as it will impact many projects.

usually the projects opt out of kotlin stdlib dependency by using kotlin.stdlib.default.dependency=false in gradle.properties.
if a project needs kotlin stdlib for compilation then a compileOnly dependency needs to be added.
for example compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.10")
the stdlib version must be compatible with the intellij platform we're building for,
see here: https://plugins.jetbrains.com/docs/intellij/kotlin.html#kotlin-standard-library

 */

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    @Suppress("UnstableApiUsage")
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(project.currentProfile().javaVersion))
        //it is possible to use jetbrains runtime for toolchain but then
        // runIde will use this runtime to run the development instance and that is not ideal,
        // it's better to run the development instance with the bundled runtime.

        //todo: but because of this issue
        // https://github.com/JetBrains/gradle-intellij-plugin/issues/1534
        // we currently must run the 241 development instance with JBR 17, until the issue is fixed
        // another issue is that runIde changes the test task executable to the bundled JBR, so when
        // building with 241 some of our tests will fail.

        vendor = JvmVendorSpec.JETBRAINS

        //there is not real need for vendor, any jdk should compile the project correctly.
        //amazon is recommended by jetbrains
        //vendor = JvmVendorSpec.IBM
    }
}

dependencies {
    //add the kotlin test library to all projects that apply this common-kotlin plugin.
    //so all project are ready to use kotlin test
    testImplementation(kotlin("test"))
}

tasks {

    withType<KotlinCompile> {

        doFirst {
            logger.lifecycle("compiling kotlin with jdk: ${kotlinJavaToolchain.javaVersion}")
            logger.lifecycle("Compiling kotlin with jvmTarget:${kotlinOptions.jvmTarget},apiVersion:${kotlinOptions.apiVersion},languageVersion:${kotlinOptions.languageVersion}")
        }

        kotlinOptions {
            verbose = true
            jvmTarget = project.currentProfile().javaVersion
            apiVersion = project.currentProfile().kotlinTarget
            languageVersion = project.currentProfile().kotlinTarget
        }
    }

}