package common

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion


enum class IdeFlavor { IC, IU, RD, PC, PY }


fun dynamicPlatformType(project: Project): String {

    return if (project.findProperty("buildWithRider") == "true") {
        IdeFlavor.RD.name
    } else if (project.findProperty("buildWIthUltimate") == "true") {
        IdeFlavor.IU.name
    } else if (project.findProperty("buildWithPycharm") == "true") {
        IdeFlavor.PC.name
    } else if (project.findProperty("buildWithPycharmPro") == "true") {
        IdeFlavor.PY.name
    } else {
        IdeFlavor.IC.name
    }
}

/**
 * platformPlugins necessary only when running an ide, not for compilation.
 */
fun Project.platformPlugins(): String {

    if (findProperty("platformPlugins") != null) {
        return findProperty("platformPlugins").toString()
    }

    return when (properties("platformType", this)) {
        IdeFlavor.RD.name -> "rider-plugins-appender"
        IdeFlavor.PC.name, IdeFlavor.PY.name -> ""
        IdeFlavor.IC.name, IdeFlavor.IU.name -> "com.intellij.java"

        else -> ""
    }
}


fun Project.platformVersion(): String = when (properties("platformType", this)) {
    IdeFlavor.RD.name -> currentProfile().riderVersion
    IdeFlavor.PC.name, IdeFlavor.PY.name -> currentProfile().pycharmVersion
    IdeFlavor.IC.name, IdeFlavor.IU.name -> currentProfile().platformVersion

    else -> currentProfile().platformVersion
}

fun Project.shouldDownloadSources(): Boolean {
    return if (findProperty("doNotDownloadSources") == null) true else false
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

    enum class Profiles { p223, p231, p232, p233 }

    fun currentProfile(project: Project): BuildProfile {

        val selectedProfile = project.findProperty("buildProfile")?.let {

            var profileToUse = it as String
            if (profileAliases.containsKey(it)) {
                profileToUse = profileAliases[it] as String
            }

            Profiles.valueOf(profileToUse)

        } ?: Profiles.p223

        return profiles[selectedProfile] ?: throw GradleException("can not find profile $selectedProfile")
    }

    //update as new profiles are added or removed
    private val profileAliases = mapOf(
        "lowest" to Profiles.p223.name,
        "latest" to Profiles.p232.name,
        "eap" to Profiles.p233.name,
    )


    private val profiles = mapOf(

        Profiles.p223 to BuildProfile(
            profile = Profiles.p223,
            // platformVersion is Intellij IDEA Community and Ultimate
            platformVersion = "2022.3.3",
            riderVersion = "2022.3.3",
            pycharmVersion = "2022.3.3",
            riderResharperVersion = "2022.3.3",
            riderResharperVersionConstant = "PROFILE_2022_3",
            pythonPluginVersion = "223.8836.26",
            platformVersionCode = "223",
            pluginSinceBuild = "223",
            pluginUntilBuild = "223.*",
            versionToRunPluginVerifier = "2022.3.3",
            kotlinTarget = KotlinVersion.KOTLIN_1_7.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),

        Profiles.p231 to BuildProfile(
            profile = Profiles.p231,
            // platformVersion is Intellij IDEA Community and Ultimate
            platformVersion = "2023.1.5",
            riderVersion = "2023.1.4",
            pycharmVersion = "2023.1.4",
            riderResharperVersion = "2023.1.4",
            riderResharperVersionConstant = "PROFILE_2023_1",
            pythonPluginVersion = "231.8770.65",
            platformVersionCode = "231",
            pluginSinceBuild = "231",
            pluginUntilBuild = "231.*",
            versionToRunPluginVerifier = "2023.1.4",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),

        Profiles.p232 to BuildProfile(
            profile = Profiles.p232,
            // platformVersion is Intellij IDEA Community and Ultimate
            platformVersion = "2023.2.2",
            riderVersion = "2023.2.1",
            pycharmVersion = "2023.2.1",
            riderResharperVersion = "2023.2.0",
            riderResharperVersionConstant = "PROFILE_2023_2",
            pythonPluginVersion = "232.9559.62",
            platformVersionCode = "232",
            pluginSinceBuild = "232",
            pluginUntilBuild = "232.*",
            versionToRunPluginVerifier = "2023.2.1",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),


        //todo: the next EAP profile. still not active and not built in github, versions are of p232 until EAP is started.
        // update when 2023.3 starts and add to github workflows
        Profiles.p233 to BuildProfile(

            profile = Profiles.p233,
            isEAP = true,
            // platformVersion is Intellij IDEA Community and Ultimate
            platformVersion = "2023.2.1",
            riderVersion = "2023.2.1",
            pycharmVersion = "2023.2.1",
            riderResharperVersion = "2023.2.0",
            riderResharperVersionConstant = "PROFILE_2023_2",
            pythonPluginVersion = "232.9559.62",
            platformVersionCode = "232",
            pluginSinceBuild = "232",
            pluginUntilBuild = "232.*",
            versionToRunPluginVerifier = "2023.2",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        )


    )

}


data class BuildProfile(
    val profile: BuildProfiles.Profiles,
    val isEAP: Boolean = false,
    val platformVersion: String,
    val riderVersion: String,
    val pycharmVersion: String,
    val riderResharperVersion: String,
    val riderResharperVersionConstant: String,
    val pythonPluginVersion: String,
    val platformVersionCode: String,
    val pluginSinceBuild: String,
    val pluginUntilBuild: String,
    val versionToRunPluginVerifier: String,
    val kotlinTarget: String,
    val javaVersion: String,
)