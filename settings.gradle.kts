import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {

    includeBuild("common-build-logic")

    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://cache-redirector.jetbrains.com/plugins.gradle.org")
        maven(url = "https://cache-redirector.jetbrains.com/myget.org.rd-snapshots.maven")
        maven(url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
        gradlePluginPortal()
    }

}


plugins {
    id("rdgen-version-settings")
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    id("org.jetbrains.intellij.platform.settings") version "2.6.0"
}

/*
kotlin-stdlib: the kotlin-stdlib must be compatible with the intellij platform version,
 see: https://plugins.jetbrains.com/docs/intellij/kotlin.html#kotlin-standard-library
 */
dependencyResolutionManagement {

    //configure repositories for all projects
    @Suppress("UnstableApiUsage")
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
            jetbrainsRuntime()
            intellijDependencies()
        }
    }



    versionCatalogs {
        create("libs") {
            //rdgen version is independent of rider version.
            //we change the rdgen version depending on the rider version we build for. see in rider and protocol
            // modules and in plugins resolutionStrategy
            version("rider-rdgen", "2023.3.2")
            library("rdGen", "com.jetbrains.rd", "rd-gen").versionRef("rider-rdgen")

            //kotlin stdlib is not packaged with the plugin because intellij platform already contains it.
            //it's necessary for compilation in some cases for example rider protocol module.
            //it must target the lowest bundled stdlib version of the platform we support
            //see: https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#adding-kotlin-support
            //see also resolutionStrategy in buildSrc/src/main/kotlin/digma-base.gradle.kts
            //before changing this version search the project for mentions of the library and make the necessary changes.
            version("kotlin-stdlib", "1.9.22")
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef("kotlin-stdlib")
            library("kotlin-stdlib-jdk8", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8").versionRef("kotlin-stdlib")
            library("kotlin-stdlib-jdk7", "org.jetbrains.kotlin", "kotlin-stdlib-jdk7").versionRef("kotlin-stdlib")

            library("guava", "com.google.guava", "guava").version("31.1-jre")
            version("retrofit", "2.9.0")
            library("retrofit-client", "com.squareup.retrofit2", "retrofit").versionRef("retrofit")
            library("retrofit-jackson", "com.squareup.retrofit2", "converter-jackson").versionRef("retrofit")
            library("retrofit-scalars", "com.squareup.retrofit2", "converter-scalars").versionRef("retrofit")
            //make sure okhttp is the same version as retrofit brings as dependency. check it with dependencies task
            version("okhttp", "3.14.9")
            library("okhttp-logging", "com.squareup.okhttp3", "logging-interceptor").versionRef("okhttp")
            library("okhttp", "com.squareup.okhttp3", "okhttp").versionRef("okhttp")
            library("okhttp-mockwebserver", "com.squareup.okhttp3", "mockwebserver").versionRef("okhttp")
            library("jackson-datetime", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").version("2.10.5")
            library("prettytime", "org.ocpsoft.prettytime", "prettytime").version("5.0.3.Final")
            library("threeten", "org.threeten", "threeten-extra").version("1.7.0")
            library("commons-lang3", "org.apache.commons", "commons-lang3").version("3.12.0")
            library("commons-collections4", "org.apache.commons", "commons-collections4").version("4.4")
            library("commons-validator", "commons-validator", "commons-validator").version("1.9.0")
            library("freemarker", "org.freemarker", "freemarker").version("2.3.30")
            library("posthog", "com.posthog.java", "posthog").version("1.1.0")
            library("google-gson", "com.google.code.gson", "gson").version("2.10.1")
            library("maven-artifact", "org.apache.maven", "maven-artifact").version("3.9.2")
            library("glovoapp-versioning", "com.glovoapp.gradle", "versioning").version("1.1.10")
            library("byte-buddy", "net.bytebuddy", "byte-buddy").version("1.14.13")
        }
    }
}

rootProject.name = "digma-intellij-plugin"
include("rider:protocol")
findProject(":rider:protocol")?.name = "protocol"
include("model", "analytics-provider", "ide-common", "jvm-common", "gradle-support", "maven-support", "rider", "kotlin", "python")
