pluginManagement {

    repositories {
        gradlePluginPortal()
        maven(url = "https://cache-redirector.jetbrains.com/plugins.gradle.org")
        maven(url = "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://cache-redirector.jetbrains.com/myget.org.rd-snapshots.maven")
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

 todo: maybe use java-platforms instead of versionCatalogs
 */
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("junit", "5.8.2")
            version("kotlin-stdlib", "1.5.10")
            library("kotlin-stdlib-jdk8", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8").versionRef("kotlin-stdlib")
            version("resteasy", "6.0.1.Final")
            library("resteasy-client", "org.jboss.resteasy", "resteasy-client").versionRef("resteasy")
            library("resteasy-jackson2-provider","org.jboss.resteasy","resteasy-jackson2-provider").versionRef("resteasy")
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
