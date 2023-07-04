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
//        this.languageVersion.set(JavaLanguageVersion.of(properties("javaVersion", project)))
        this.languageVersion.set(JavaLanguageVersion.of(project.currentProfile().javaVersion))
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
            logger.lifecycle("Building kotlin with jvmTarget:${kotlinOptions.jvmTarget},apiVersion:${kotlinOptions.apiVersion},jvmTarget:${kotlinOptions.languageVersion}")
        }

        kotlinOptions {
            jvmTarget = project.currentProfile().javaVersion
            apiVersion = project.currentProfile().kotlinTarget
            languageVersion = project.currentProfile().kotlinTarget
        }
    }

//    properties("javaVersion", project).let {
//        withType<KotlinCompile> {
//            kotlinOptions.jvmTarget = it
//        }
//    }
}