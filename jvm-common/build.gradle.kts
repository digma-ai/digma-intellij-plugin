import common.IdeFlavor
import common.logBuildProfile
import common.platformVersion
import common.shouldDownloadSources
import de.undercouch.gradle.tasks.download.Download
import java.util.Properties

plugins {
    id("plugin-library")
}


dependencies {
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))

    testImplementation(project(":ide-common"))
}

//jvm module should always build with IC
val platformType by extra(IdeFlavor.IC.name)

logBuildProfile(project)

intellij {
    version.set("$platformType-${project.platformVersion()}")
    plugins.set(listOf("com.intellij.java", "org.jetbrains.kotlin"))
    downloadSources.set(project.shouldDownloadSources())
}


tasks {

    val downloadOtelJars = register("downloadOtelJars", Download::class.java) {

        val properties = Properties()
        properties.load(layout.projectDirectory.file("src/main/resources/jars-urls.properties").asFile.inputStream())

        src(
            listOf(
                properties.getProperty("otel-agent"),
                properties.getProperty("digma-extension")
            )
        )

        logger.lifecycle("copying jars $properties")

        dest(File(project.sourceSets.main.get().output.resourcesDir, "otelJars"))
        overwrite(true)
    }

    processResources {
        dependsOn(downloadOtelJars)
    }
}
