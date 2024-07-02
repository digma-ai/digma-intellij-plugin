import common.BuildProfiles
import common.currentProfile
import common.dynamicPlatformType
import common.platformVersion
import common.withSilenceLogging
import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import java.util.Properties

plugins {
    id("plugin-library")
}

//todo: modules that need to build with Idea can always use IC , there is no real need to build with IU
//this module should always build with IC or IU.
//if building with buildWithRider=true then this module should not use the dynamic type.
// it should use the dynamic type only when building with buildWIthUltimate=true
//platformType impacts project.platformVersion() so it must be accurate.
val platformType: IntelliJPlatformType by extra {
    if (dynamicPlatformType(project) == IntelliJPlatformType.IntellijIdeaUltimate){
        IntelliJPlatformType.IntellijIdeaUltimate
    }else{
        IntelliJPlatformType.IntellijIdeaCommunity
    }
}

dependencies {
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))

    testImplementation(project(":ide-common"))

    intellijPlatform {

        //this module can only build with IC or IU, so only support replacing to IU when
        // we build with IU , otherwise build with IC even if platformType is something else like RD or PY
        if (platformType == IntelliJPlatformType.IntellijIdeaUltimate) {
            intellijIdeaUltimate(project.platformVersion())
        } else {
            intellijIdeaCommunity(project.platformVersion())
        }

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
    }
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

        withSilenceLogging {
            logger.lifecycle("${project.name}: jars to download $properties")
        }


        dest(File(project.sourceSets.main.get().output.resourcesDir, "otelJars"))
        overwrite(false)
        onlyIfModified(true)
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
