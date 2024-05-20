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
                properties.getProperty("digma-extension"),
                properties.getProperty("digma-agent")
            )
        )

        logger.lifecycle("jars to download $properties")

        dest(File(project.sourceSets.main.get().output.resourcesDir, "otelJars"))
        overwrite(true)
        //if a jar is downloaded with version then its name needs to change. it may happen
        // in development if the url for some of the jars is changed to download from somewhere else.
        //usually latest jar is downloaded without version
        eachFile {
            name = if (name.startsWith("digma-otel-agent-extension", true)) {
                "digma-otel-agent-extension.jar"
            } else if (name.startsWith("digma-agent", true)) {
                "digma-agent.jar"
            } else if (name.startsWith("opentelemetry-javaagent", true)) {
                "opentelemetry-javaagent.jar"
            } else {
                name
            }
        }
    }

    processResources {
        dependsOn(downloadOtelJars)
    }
}
