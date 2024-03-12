import common.IdeFlavor
import common.logBuildProfile
import common.platformVersion
import common.shouldDownloadSources
import de.undercouch.gradle.tasks.download.Download

plugins {
    id("plugin-library")
}


dependencies {
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))
}

//jvm module should always build with IC
val platformType by extra(IdeFlavor.IC.name)

logBuildProfile(project)

intellij {
    version.set("$platformType-${project.platformVersion()}")
    plugins.set(listOf("com.intellij.java", "org.jetbrains.kotlin", "org.jetbrains.idea.maven", "org.jetbrains.plugins.gradle"))
    downloadSources.set(project.shouldDownloadSources())
}


tasks {

    val downloadOtelJars = register("downloadOtelJars", Download::class.java) {
        src(
            listOf(
//              "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.1.0/opentelemetry-javaagent.jar",
                "https://github.com/digma-ai/otel-java-instrumentation/releases/latest/download/digma-otel-agent-extension.jar"
            )
        )

        dest(File(project.sourceSets.main.get().output.resourcesDir, "otelJars"))
        overwrite(false)
    }

    processResources {
        dependsOn(downloadOtelJars)
    }
}
