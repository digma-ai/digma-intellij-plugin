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
kotlin jvm plugin: kotlin jvm plugin is added where necessary by applying the common script plugin
 id("common-kotlin"), and because it's a buildSrc implementation it can not be added to a project with version.
 if really necessary to add kotlin to a project, and it's not possible to use id("common-kotlin") then use kotlin("jvm) without version.
kotlin-stdlib: the kotlin-stdlib must be compatible with the intellij platform version,
 see: https://plugins.jetbrains.com/docs/intellij/kotlin.html#kotlin-standard-library

 todo: maybe use java-platforms instead of versionCatalogs
 */
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
//            version("kotlin-jvm", "1.6.10")
//            plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin-jvm")
            version("kotlin-stdlib","1.5.10")
            library("kotlin-stdlib-jdk8", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8").versionRef("kotlin-stdlib")
        }
    }
}


rootProject.name = "digma-intellij-plugin"
include("rider:protocol")
findProject(":rider:protocol")?.name = "protocol"
include("model","analytics-provider","ide-common","idea","pycharm","rider")
