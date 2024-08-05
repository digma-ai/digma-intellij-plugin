import common.dynamicPlatformType
import common.platformVersion
import common.useBinaryInstaller
import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("plugin-library")
}

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


tasks {

    val downloadComposeFile = register("downloadComposeFile", Download::class.java) {
        src(
            listOf(
                "https://get.digma.ai/"
            )
        )

        val dir = File(project.sourceSets.main.get().output.resourcesDir, "docker-compose")
        dest(File(dir, "docker-compose.yml"))
        overwrite(false)
        onlyIfModified(true)
    }

    processResources {
        dependsOn(downloadComposeFile)
    }
}