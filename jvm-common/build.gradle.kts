import common.BuildProfiles
import common.BuildProfiles.greaterThan
import common.currentProfile
import common.dynamicPlatformType
import common.platformVersion
import common.useBinaryInstaller
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

plugins {
    id("plugin-library")
}

val generatedResourcesDirName = "generated-resources"

//todo: modules that need to build with Idea can always use IC , there is no real need to build with IU
//this module should always build with IC or IU.
//if building with buildWithRider=true then this module should not use the dynamic type.
// it should use the dynamic type only when building with buildWIthUltimate=true
//platformType impacts project.platformVersion() so it must be accurate.
val platformType: IntelliJPlatformType by extra {
    if (dynamicPlatformType(project) == IntelliJPlatformType.IntellijIdeaUltimate) {
        IntelliJPlatformType.IntellijIdeaUltimate
    } else {
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
            intellijIdeaUltimate(project.platformVersion(), project.useBinaryInstaller())
        } else {
            intellijIdeaCommunity(project.platformVersion(), project.useBinaryInstaller())
        }

        bundledPlugin("com.intellij.java")

        //todo: this is a workaround, this module should be bundled.
        // check in next version if it is still necessary.
        if (project.currentProfile().profile.greaterThan(BuildProfiles.Profile.p241)) {
            bundledModule("intellij.platform.collaborationTools")
        }
    }
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.dir(generatedResourcesDirName))
        }
    }
}

tasks {

    val downloadOtelJars by registering {

        val propsFile = layout.projectDirectory.file("src/main/resources/jars-urls.properties")
        val outputDirProperty = layout.buildDirectory.file("$generatedResourcesDirName/otelJars")

        inputs.file(propsFile)
        outputs.dir(outputDirProperty)

        doLast {
            val props = Properties().apply {
                load(propsFile.asFile.inputStream())
            }

            val outputDir = outputDirProperty.get().asFile
            outputDir.mkdirs()

            val nameMap = mapOf(
                "otel-agent" to "opentelemetry-javaagent.jar",
                "digma-extension" to "digma-otel-agent-extension.jar",
                "digma-agent" to "digma-agent.jar"
            )

            props.forEach { (key, value) ->
                val finalName = nameMap[key] ?: throw GradleException("Unknown key in properties file: $key")
                val url = value.toString()
                val outputFile = outputDir.resolve(finalName)

                logger.lifecycle("Downloading $url → ${outputFile.name}")
                URI(url).toURL().openStream().use { input ->
                    Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }


    processResources {
        dependsOn(downloadOtelJars)
    }
}
