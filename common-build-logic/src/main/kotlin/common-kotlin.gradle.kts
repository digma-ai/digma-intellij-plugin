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
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(project.currentProfile().javaVersion))
        //don't configure 'vendor = JvmVendorSpec.JETBRAINS'
        // see this issue: https://github.com/JetBrains/gradle-intellij-plugin/issues/1538
        vendor = JvmVendorSpec.ADOPTIUM
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
            logger.lifecycle("${name}: Compiling kotlin with jdk: ${kotlinJavaToolchain.javaVersion.get()}, " +
                    "jvmTarget:${compilerOptions.jvmTarget.get()},apiVersion:${compilerOptions.apiVersion.get()},languageVersion:${compilerOptions.languageVersion.get()}")
        }

        compilerOptions {
            verbose = true
            apiVersion.set(project.currentProfile().kotlinTarget)
            languageVersion.set(project.currentProfile().kotlinTarget)
            jvmTarget.set(project.currentProfile().kotlinJvmTarget)
        }
    }
}