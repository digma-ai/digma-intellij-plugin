@file:Suppress("unused")

package common

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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

//we can use binary installers but not for EAP because EAP is not published to jetbrains CDN
fun Project.useBinaryInstaller(): Boolean = !this.currentProfile().isEAP


object BuildProfiles {

    @Suppress("EnumEntryName")
    enum class Profile { p241, p242, p243, p251, p252 }

    fun Profile.greaterThan(other:Profile):Boolean{
        val thisNumber = this.name.substring(1).toInt()
        val otherNumber = other.name.substring(1).toInt()
        return thisNumber > otherNumber
    }

    fun Profile.lowerThan(other:Profile):Boolean{
        val thisNumber = this.name.substring(1).toInt()
        val otherNumber = other.name.substring(1).toInt()
        return thisNumber < otherNumber
    }


    fun currentProfile(project: Project): BuildProfile {

        val selectedProfile = project.findProperty("buildProfile")?.let {

            var profileToUse = it as String
            if (profileAliases.containsKey(it)) {
                profileToUse = profileAliases[it] as String
            }

            Profile.valueOf(profileToUse)

        } ?: Profile.p241

        return profiles[selectedProfile] ?: throw GradleException("can not find profile $selectedProfile")
    }

    //update this list as new profiles are added or removed
    private val profileAliases = mapOf(
        "lowest" to Profile.p241.name,
        "latest" to Profile.p251.name,
        "eap" to Profile.p252.name,
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

        //the java and kotlin version required for each release is here:
        //see https://github.com/JetBrains/intellij-platform-gradle-plugin/blob/92e4348bcc64d6b958f7fb53f043aa61719566ca/src/main/kotlin/org/jetbrains/intellij/platform/gradle/utils/PlatformJavaVersions.kt
        ///see https://github.com/JetBrains/intellij-platform-gradle-plugin/blob/92e4348bcc64d6b958f7fb53f043aa61719566ca/src/main/kotlin/org/jetbrains/intellij/platform/gradle/utils/PlatformKotlinVersions.kt


        //see building-how-to/*


        Profile.p241 to BuildProfile(
            profile = Profile.p241,
            platformVersion = "2024.1.7",
            riderVersion = "2024.1.6",
            pycharmVersion = "2024.1.5",
            riderTargetFramework = "net8.0",
            riderResharperVersionConstant = "",
            platformVersionCode = "241",
            pluginSinceBuild = "241",
            pluginUntilBuild = "241.*",
            kotlinTarget = KotlinVersion.KOTLIN_1_9,
            kotlinJvmTarget = JvmTarget.JVM_17,
            javaVersion = JavaVersion.VERSION_17.majorVersion,
        ),



        Profile.p242 to BuildProfile(
            profile = Profile.p242,
            platformVersion = "2024.2.4",
            riderVersion = "2024.2.7",
            pycharmVersion = "2024.2",
            riderTargetFramework = "net8.0",
            riderResharperVersionConstant = "",
            platformVersionCode = "242",
            pluginSinceBuild = "242",
            pluginUntilBuild = "242.*",
            kotlinTarget = KotlinVersion.KOTLIN_1_9,
            kotlinJvmTarget = JvmTarget.JVM_17,
            javaVersion = JavaVersion.VERSION_17.majorVersion,
        ),

        Profile.p243 to BuildProfile(
            profile = Profile.p243,
            platformVersion = "2024.3.5",
            riderVersion = "2024.3.6",
            pycharmVersion = "2024.3",
            riderTargetFramework = "net8.0",
            riderResharperVersionConstant = "PROFILE_2024_3",
            platformVersionCode = "243",
            pluginSinceBuild = "243",
            pluginUntilBuild = "243.*",
            kotlinTarget = KotlinVersion.KOTLIN_2_0,
            kotlinJvmTarget = JvmTarget.JVM_21,
            javaVersion = JavaVersion.VERSION_21.majorVersion,
        ),

        Profile.p251 to BuildProfile(
            profile = Profile.p251,
            platformVersion = "2025.1.1.1",
            riderVersion = "2025.1.2",
            pycharmVersion = "2025.1",
            riderTargetFramework = "net8.0",
            riderResharperVersionConstant = "PROFILE_2024_3",
            platformVersionCode = "251",
            pluginSinceBuild = "251",
            pluginUntilBuild = "251.*",
            kotlinTarget = KotlinVersion.KOTLIN_2_0,
            kotlinJvmTarget = JvmTarget.JVM_21,
            javaVersion = JavaVersion.VERSION_21.majorVersion,
        ),

        Profile.p252 to BuildProfile(
            isEAP = true,
            profile = Profile.p252,
            platformVersion = "252.13776.59-EAP-SNAPSHOT",
            riderVersion = "2025.2-EAP1-SNAPSHOT",
            pycharmVersion = "252-EAP-SNAPSHOT",
            riderTargetFramework = "net8.0",
            riderResharperVersionConstant = "PROFILE_2024_3,PROFILE_2025_2",
            platformVersionCode = "252",
            pluginSinceBuild = "252",
            pluginUntilBuild = "252.*",
            kotlinTarget = KotlinVersion.KOTLIN_2_0,
            kotlinJvmTarget = JvmTarget.JVM_21,
            javaVersion = JavaVersion.VERSION_21.majorVersion,
        )

    )

}

data class BuildProfile(
    val profile: BuildProfiles.Profile,
    val isEAP: Boolean = false,
    val platformVersion: String,
    val riderVersion: String,
    val pycharmVersion: String,
    val riderTargetFramework: String,
    val riderResharperVersionConstant: String,
    val platformVersionCode: String,
    val pluginSinceBuild: String,
    val pluginUntilBuild: String,
    val kotlinTarget: KotlinVersion,
    val kotlinJvmTarget: JvmTarget,
    val javaVersion: String,
)