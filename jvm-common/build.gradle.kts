import common.BuildProfiles
import common.BuildProfiles.greaterThan
import common.currentProfile
import common.dynamicPlatformType
import common.platformVersion
import common.useBinaryInstaller
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


//prepare properties for downloadOtelJars during the configuration phase to support configuration cache
val jarsUrls: List<String> by lazy {
    val propsFile = layout.projectDirectory.file("src/main/resources/jars-urls.properties").asFile
    Properties().apply {
        propsFile.inputStream().use { load(it) }
    }.values.toList().map { it.toString() }
}
val otelJarTmpDir = layout.buildDirectory.dir("generated/otelJars")
val resourceOtelJarDir: Provider<File> = provider {
    val resourcesDir = project.the<SourceSetContainer>()["main"].output.resourcesDir
        ?: error("resourcesDir is not set. You may need to set it manually or use processResources destinationDir.")
    File(resourcesDir, "otelJars")
}

tasks {

    val downloadOtelJars = register("downloadOtelJars", Download::class.java) {
        notCompatibleWithConfigurationCache("downloadOtelJars is not yet compatible with configuration cache")

        src(jarsUrls)
        dest(otelJarTmpDir)
        overwrite(false)
        onlyIfModified(true)
        doFirst {
            withSilenceLogging {
                logger.lifecycle("$name: jars to download $jarsUrls")
            }
        }
    }

    val renameOtelJars = register<Copy>("renameOtelJars") {
        //strip versions from the jars

        dependsOn(downloadOtelJars)

        from(otelJarTmpDir)
        into(resourceOtelJarDir)

        rename(".*digma-otel-agent-extension.*", "digma-otel-agent-extension.jar")
        rename(".*digma-agent.*", "digma-agent.jar")
        rename(".*opentelemetry-javaagent.*", "opentelemetry-javaagent.jar")
    }



    processResources {
        dependsOn(renameOtelJars)
    }
}
