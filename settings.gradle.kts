pluginManagement {

    repositories {
        gradlePluginPortal()
        maven(url = "https://cache-redirector.jetbrains.com/plugins.gradle.org")
        maven(url = "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://cache-redirector.jetbrains.com/myget.org.rd-snapshots.maven")
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.jetbrains.rdgen") {
                useModule("com.jetbrains.rd:rd-gen:${requested.version}")
            }
        }
    }
}


/*
kotlin-stdlib: the kotlin-stdlib must be compatible with the intellij platform version,
 see: https://plugins.jetbrains.com/docs/intellij/kotlin.html#kotlin-standard-library

we currently have to specify intellij platform version in a few places , so use
the version alias where ever possible

 todo: maybe use java-platforms instead of versionCatalogs
 */
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("intellij-platform", "2022.1.1")
            version("junit", "5.8.2")
            version("kotlin-stdlib", "1.6.20")
            library("kotlin-stdlib-jdk8", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8").versionRef("kotlin-stdlib")
            version("retrofit", "2.9.0")
            library("retrofit-client", "com.squareup.retrofit2", "retrofit").versionRef("retrofit")
            library("retrofit-jackson","com.squareup.retrofit2","converter-jackson").versionRef("retrofit")
            library("logging-interceptor","com.squareup.retrofit2","converter-jackson").versionRef("retrofit")
            version("okhttp", "4.9.3")
            library("okhttp", "com.squareup.okhttp3", "okhttp").versionRef("okhttp")
            library("okhttp-mockwebserver", "com.squareup.okhttp3", "mockwebserver").versionRef("okhttp")
        }
    }
}


rootProject.name = "digma-intellij-plugin"
include("rider:protocol")
findProject(":rider:protocol")?.name = "protocol"
include("model","analytics-provider","ide-common","idea","pycharm","rider")
