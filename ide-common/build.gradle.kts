import common.BuildProfiles
import common.BuildProfiles.greaterThan
import common.currentProfile
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
        //todo: this is a workaround for plugin 2.1.0, this module should come as transitive dependency of Git4Idea.
        // check in next version if it is still necessary. discussed here:
        // https://jetbrains-platform.slack.com/archives/C05C80200LS/p1727279837850189
        // https://jetbrains-platform.slack.com/archives/C05C80200LS/p1730794028550679
        if (project.currentProfile().profile.greaterThan(BuildProfiles.Profile.p241)) {
            bundledModule("intellij.platform.vcs.dvcs.impl")
        }
    }
}


tasks {

    val uiVersionFile = project.rootProject.file("ui-version")
    val uiVersion = uiVersionFile.readText()
    //the directory inside the jar to package to
    val uiBundleDir = File(project.sourceSets.main.get().output.resourcesDir, "ui-bundle")
    val uiBundleFile = File(uiBundleDir, "digma-ui-$uiVersion.zip")

    val downloadUiBundle by registering(Download::class) {

        inputs.files(uiVersionFile)
        outputs.files(uiBundleFile)

        src(
            listOf(
                "https://github.com/digma-ai/digma-ui/releases/download/v$uiVersion/dist-jetbrains-v$uiVersion.zip"
            )
        )
        dest(uiBundleFile)
        retries(3)
    }

    val createUiBundleVersionFile by registering(Copy::class) {
        from(uiVersionFile)
        into(uiBundleDir)
    }


    processResources {
        dependsOn(downloadUiBundle, createUiBundleVersionFile)
    }
}