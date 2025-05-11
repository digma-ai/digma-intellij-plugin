import common.DownloadUiBundle
import common.GENERATED_RESOURCES_DIR_NAME
import common.UI_BUNDLE_DIR_NAME
import common.UI_VERSION_FILE_NAME
import common.dynamicPlatformType
import common.platformVersion
import common.useBinaryInstaller
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

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.dir(GENERATED_RESOURCES_DIR_NAME))
        }
    }
}

tasks {

    val downloadUiBundle  by registering(DownloadUiBundle::class) {
        uiVersionFile.set(project.rootProject.file(UI_VERSION_FILE_NAME))
        outputDir.set(layout.buildDirectory.dir(UI_BUNDLE_DIR_NAME))
    }

    val createUiBundleVersionFile by registering(Copy::class) {
        from(project.rootProject.file(UI_VERSION_FILE_NAME))
        into(layout.buildDirectory.dir(UI_BUNDLE_DIR_NAME))
    }

    processResources {
        dependsOn(downloadUiBundle, createUiBundleVersionFile)
    }
}