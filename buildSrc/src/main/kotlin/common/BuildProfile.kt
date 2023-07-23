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

    enum class Profiles { lowest, latest, eap }

    fun currentProfile(project: Project): BuildProfile {

        val selectedProfile = project.findProperty("buildProfile")?.let {
            Profiles.valueOf(it as String)
        } ?: Profiles.lowest

        return profiles[selectedProfile] ?: throw GradleException("can not find profile $selectedProfile")
    }


    private val profiles = mapOf(

        Profiles.lowest to BuildProfile(
            profile = Profiles.lowest,
            // platformVersion is Intellij IDEA Community and Ultimate
            platformVersion = "2022.3.1",
            riderVersion = "2022.3.1",
            pycharmVersion = "2022.3.1",
            riderResharperVersion = "2022.3.1",
            riderResharperVersionConstant = "PROFILE_2022_3",
            pythonPluginVersion = "223.7571.182",
            platformVersionCode = "223",
            pluginSinceBuild = "223",
            pluginUntilBuild = "223.*",
            versionToRunPluginVerifier = "2022.3.1",
            kotlinTarget = KotlinVersion.KOTLIN_1_7.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),

        Profiles.latest to BuildProfile(
            profile = Profiles.latest,
            // platformVersion is Intellij IDEA Community and Ultimate
            platformVersion = "2023.1.4",
            riderVersion = "2023.1.4",
            pycharmVersion = "2023.1.4",
            riderResharperVersion = "2023.1.3",
            riderResharperVersionConstant = "PROFILE_2023_1",
            pythonPluginVersion = "231.8770.65",
            platformVersionCode = "231",
            pluginSinceBuild = "231",
            pluginUntilBuild = "231.*",
            versionToRunPluginVerifier = "2023.1.3",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),

        Profiles.eap to BuildProfile(
            profile = Profiles.eap,
            // platformVersion is Intellij IDEA Community and Ultimate
            platformVersion = "232-EAP-SNAPSHOT",
            riderVersion = "2023.2-SNAPSHOT",
            pycharmVersion = "232-EAP-SNAPSHOT",
            riderResharperVersion = "2023.2.0-eap10",
            riderResharperVersionConstant = "PROFILE_2023_2",
            pythonPluginVersion = "232.8660.142",
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