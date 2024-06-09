package common

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

//todo: some functions here are marked as not used but they are used. its an intellij issue.
// probably because its an included build. when it was buildSrc it was OK.
// maybe open a bug report in youtrack ?

fun dynamicPlatformType(project: Project): IntelliJPlatformType {

    return if (project.findProperty("buildWithRider") == "true") {
        IntelliJPlatformType.Rider
    } else if (project.findProperty("buildWIthUltimate") == "true") {
        IntelliJPlatformType.IntellijIdeaUltimate
    } else if (project.findProperty("buildWithPycharm") == "true") {
        IntelliJPlatformType.PyCharmCommunity
    } else if (project.findProperty("buildWithPycharmPro") == "true") {
        IntelliJPlatformType.PyCharmProfessional
    } else {
        IntelliJPlatformType.IntellijIdeaCommunity
    }
}


//this method is not necessary anymore. specific modules depend on specific plugins.
///**
// * platformPlugins necessary for compilation and for runIde.
// * should return bundled plugins. for example java and kotlin.
// */
//fun Project.platformPlugins(): String {
//
//    if (findProperty("platformPlugins") != null) {
//        return findProperty("platformPlugins").toString()
//    }
//
//    return when (platformTypeProperty(this)) {
//        IntelliJPlatformType.Rider -> "rider-plugins-appender"
//        IntelliJPlatformType.PyCharmCommunity -> "PythonCore"
//        IntelliJPlatformType.PyCharmProfessional -> "Pythonid"
//        IntelliJPlatformType.IntellijIdeaCommunity, IntelliJPlatformType.IntellijIdeaUltimate -> "com.intellij.java,org.jetbrains.kotlin"
//
//        else -> ""
//    }
//}


fun Project.platformVersion(): String = when (platformTypeProperty(this)) {
    IntelliJPlatformType.Rider -> currentProfile().riderVersion
    IntelliJPlatformType.PyCharmCommunity, IntelliJPlatformType.PyCharmProfessional -> currentProfile().pycharmVersion
    IntelliJPlatformType.IntellijIdeaCommunity, IntelliJPlatformType.IntellijIdeaUltimate -> currentProfile().platformVersion

    else -> currentProfile().platformVersion
}


fun Project.buildVersion(): String {
    return "${common.semanticversion.getSemanticVersion(this)}+${this.currentProfile().platformVersionCode}"
}

fun Project.withCurrentProfile(consumer: (BuildProfile) -> Unit) {
    val buildProfile = BuildProfiles.currentProfile(this)
    consumer(buildProfile)
}

fun Project.currentProfile(): BuildProfile = BuildProfiles.currentProfile(this)


object BuildProfiles {

    enum class Profiles { p231, p232, p233, p241, p242 }

    fun Profiles.greaterThan(other:Profiles):Boolean{
        val thisNumber = this.name.substring(1).toInt()
        val otherNumber = other.name.substring(1).toInt()
        return thisNumber > otherNumber
    }


    fun currentProfile(project: Project): BuildProfile {

        val selectedProfile = project.findProperty("buildProfile")?.let {

            var profileToUse = it as String
            if (profileAliases.containsKey(it)) {
                profileToUse = profileAliases[it] as String
            }

            Profiles.valueOf(profileToUse)

        } ?: Profiles.p231

        return profiles[selectedProfile] ?: throw GradleException("can not find profile $selectedProfile")
    }

    //update this list as new profiles are added or removed
    private val profileAliases = mapOf(
        "lowest" to Profiles.p231.name,
        "latest" to Profiles.p241.name,
        "eap" to Profiles.p242.name,
    )


    //todo: check products releases file, can be used to automatically to update the profiles
    // https://www.jetbrains.com/updates/updates.xml
    // IntelliJPluginConstants.IDEA_PRODUCTS_RELEASES_URL
    // https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-listproductsreleases

    private val profiles = mapOf(


        //it's possible to use build numbers in platformVersion,riderVersion,pycharmVersion
        // take the build numbers from:
        //https://www.jetbrains.com/intellij-repository/releases/
        //https://www.jetbrains.com/intellij-repository/snapshots/

        //see building-how-to/*

        Profiles.p231 to BuildProfile(
            profile = Profiles.p231,
            platformVersion = "2023.1.6",
            riderVersion = "2023.1.4",
            pycharmVersion = "2023.1.5",
            riderTargetFramework = "net472",
            riderResharperVersionConstant = "PROFILE_2023_1",
            platformVersionCode = "231",
            pluginSinceBuild = "231",
            pluginUntilBuild = "231.*",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),

        Profiles.p232 to BuildProfile(
            profile = Profiles.p232,
            platformVersion = "2023.2.6",
            riderVersion = "2023.2.3",
            pycharmVersion = "2023.2.6",
            riderTargetFramework = "net472",
            riderResharperVersionConstant = "PROFILE_2023_2",
            platformVersionCode = "232",
            pluginSinceBuild = "232",
            pluginUntilBuild = "232.*",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),


        Profiles.p233 to BuildProfile(

            profile = Profiles.p233,
            platformVersion = "2023.3.4",
            riderVersion = "2023.3.3",
            pycharmVersion = "2023.3.3",
            riderTargetFramework = "net472",
            riderResharperVersionConstant = "PROFILE_2023_2",
            platformVersionCode = "233",
            pluginSinceBuild = "233",
            pluginUntilBuild = "233.*",
            kotlinTarget = KotlinVersion.KOTLIN_1_9.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),


        Profiles.p241 to BuildProfile(

            profile = Profiles.p241,
            platformVersion = "2024.1.3",
            riderVersion = "2024.1.2",
            pycharmVersion = "2024.1.2",
            riderTargetFramework = "net8.0",
            riderResharperVersionConstant = "PROFILE_2023_2",
            platformVersionCode = "241",
            pluginSinceBuild = "241",
            pluginUntilBuild = "241.*",
            kotlinTarget = KotlinVersion.KOTLIN_1_9.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion,
        ),



        //todo: probably needs java 21, see in intellij plugin PlatformJavaVersions.kt and PlatformKotlinVersions.kt
        //see https://github.com/JetBrains/intellij-platform-gradle-plugin/blob/92e4348bcc64d6b958f7fb53f043aa61719566ca/src/main/kotlin/org/jetbrains/intellij/platform/gradle/utils/PlatformJavaVersions.kt
        ///see https://github.com/JetBrains/intellij-platform-gradle-plugin/blob/92e4348bcc64d6b958f7fb53f043aa61719566ca/src/main/kotlin/org/jetbrains/intellij/platform/gradle/utils/PlatformKotlinVersions.kt
        Profiles.p242 to BuildProfile(

            isEAP = true,
            profile = Profiles.p242,
            platformVersion = "242-EAP-SNAPSHOT",
            riderVersion = "2024.2-SNAPSHOT",
            pycharmVersion = "242-EAP-SNAPSHOT",
            riderTargetFramework = "net8.0",
            riderResharperVersionConstant = "PROFILE_2023_2",
            platformVersionCode = "242",
            pluginSinceBuild = "242",
            pluginUntilBuild = "242.*",
            kotlinTarget = KotlinVersion.KOTLIN_1_9.version, //todo: maybe need to upgrade to 2.0
            javaVersion = JavaVersion.VERSION_17.majorVersion,
        )

    )

}

/*
 * Notes:
 * pythonPluginVersion:
 *  pythonPluginVersion is necessary when building the python module with type=IC, in that case the version needs to be compatible
 *  with the IC platform version. if building the python module with type=PC then pythonPluginVersion is not relevant.
 *  building with type=PC means more disk space in GitHub.
 *  currently we build python with type=PC because matching the pythonPluginVersion and latest EAP build is not always possible,
 *  sometimes it takes time before there is a compatible python plugin version. and anyway it's easier to just build with PC.
 */
data class BuildProfile(
    val profile: BuildProfiles.Profiles,
    val isEAP: Boolean = false,
    val platformVersion: String,
    val riderVersion: String,
    val pycharmVersion: String,
    val riderTargetFramework: String,
    val riderResharperVersionConstant: String,
    val platformVersionCode: String,
    val pluginSinceBuild: String,
    val pluginUntilBuild: String,
    val kotlinTarget: String,
    val javaVersion: String,
)