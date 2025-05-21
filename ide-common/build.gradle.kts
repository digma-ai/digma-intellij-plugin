import common.dynamicPlatformType
import common.platformVersion
import common.useBinaryInstaller
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("plugin-library")
}

val generatedResourcesDirName = "generated-resources"

//ide-common module should build with different platform types, if running rider with runIde or building
// with buildWithRider=true it should build with RD,
// if running ultimate or building with buildWIthUltimate=true it should build with IU, etc.
//the default is to build with IC
val platformType: IntelliJPlatformType by extra(dynamicPlatformType(project))


dependencies {

    api(libs.threeten)
    api(libs.commons.lang3)
    api(libs.commons.collections4)
    api(libs.commons.validator)
    api(libs.posthog)
    api(libs.maven.artifact)
    api(libs.glovoapp.versioning) {
        //glovoapp brings very old jetbrains annotations, and it messes up the compile-classpath.
        //we don't need its transitive dependencies, everything it needs is already in place.
        isTransitive = false
    }
    api(libs.byte.buddy)
    api(libs.jackson.datetime)

    implementation(project(":model"))
    implementation(project(":analytics-provider"))

    intellijPlatform {
        //this module uses create because it may be Idea,Rider,Pycharm etc.
        create(platformType, project.platformVersion(), project.useBinaryInstaller())
        bundledPlugin("Git4Idea")
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

    val uiVersionFile = project.rootProject.file("ui-version")
    val uiBundleDirProperty = layout.buildDirectory.dir("$generatedResourcesDirName/ui-bundle")

    val downloadUiBundle by registering{
        inputs.files(uiVersionFile)
        outputs.dir(uiBundleDirProperty)

        doLast {
            val outputDir = uiBundleDirProperty.get().asFile
            outputDir.mkdirs()

            val uiVersion = uiVersionFile.readText()
            val url = "https://github.com/digma-ai/digma-ui/releases/download/v$uiVersion/dist-jetbrains-v$uiVersion.zip"
            val uiBundleFileName = "digma-ui-$uiVersion.zip"
            val uiBundleFile = outputDir.resolve(uiBundleFileName)

            logger.lifecycle("Downloading $url → ${uiBundleFile.name}")
            URI(url).toURL().openStream().use { input ->
                Files.copy(input, uiBundleFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }


    val createUiBundleVersionFile by registering(Copy::class) {
        from(uiVersionFile)
        into(uiBundleDirProperty)
    }


    processResources {
        dependsOn(downloadUiBundle, createUiBundleVersionFile)
    }
}