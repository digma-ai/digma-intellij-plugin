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
fun Project.platformPlugins(): String = when (properties("platformType", this)) {
    IdeFlavor.RD.name -> "rider-plugins-appender"
    IdeFlavor.PC.name,
    IdeFlavor.PY.name,
    -> ""

    IdeFlavor.IC.name,
    IdeFlavor.IU.name,
    -> "com.intellij.java"

    else -> ""
}


fun Project.platformVersion(): String = when (properties("platformType", this)) {
    IdeFlavor.RD.name -> currentProfile().riderVersion
    IdeFlavor.PC.name,
    IdeFlavor.PY.name,
    -> currentProfile().pycharmVersion

    IdeFlavor.IC.name,
    IdeFlavor.IU.name,
    -> currentProfile().platformVersion

    else -> currentProfile().platformVersion
}


fun Project.buildVersion(): String {
    return "${common.semanticversion.getSemanticVersion(this)}.${this.currentProfile().platformVersionCode}"
}

fun Project.withCurrentProfile(consumer: (BuildProfile) -> Unit) {
    val buildProfile = BuildProfiles.currentProfile(this)
    consumer(buildProfile)
}

fun Project.currentProfile(): BuildProfile = BuildProfiles.currentProfile(this)


object BuildProfiles {

    fun currentProfile(project: Project): BuildProfile {
        val profile = project.findProperty("buildProfile") ?: "default"
        return profiles[profile] ?: throw GradleException("can not find profile $profile")
    }


    private val profiles = mapOf(

        "default" to BuildProfile(
            platformVersion = "2022.3.1",
            riderVersion = "2022.3.1",
            pycharmVersion = "2022.3.1",
            riderResharperVersion = "2022.3.1",
            riderResharperVersionConstant = "PROFILE_2022_3",
            pythonPluginVersion = "223.7571.182",
            platformVersionCode = "223",
            pluginSinceBuild = "223",
            pluginUntilBuild = "223.*",
            kotlinTarget = KotlinVersion.KOTLIN_1_7.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),

        "latest" to BuildProfile(
            platformVersion = "2023.1.3",
            riderVersion = "2023.1.3",
            pycharmVersion = "2023.1.3",
            riderResharperVersion = "2023.1.3",
            riderResharperVersionConstant = "PROFILE_2023_1",
            pythonPluginVersion = "231.8770.65",
            platformVersionCode = "231",
            pluginSinceBuild = "231",
            pluginUntilBuild = "231.*",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),

        "eap" to BuildProfile(
            platformVersion = "LATEST-EAP-SNAPSHOT",
//            platformVersion = "232-EAP-SNAPSHOT",
//            platformVersion = "232.8660-EAP-CANDIDATE-SNAPSHOT",
            riderVersion = "2023.2-EAP7-SNAPSHOT",
            pycharmVersion = "LATEST-EAP-SNAPSHOT",
            riderResharperVersion = "2023.2.0-eap07",
            riderResharperVersionConstant = "PROFILE_2023_2",
//            pythonPluginVersion = "232.8296.17",
            pythonPluginVersion = "232.8453.116",
            platformVersionCode = "232",
            pluginSinceBuild = "232",
            pluginUntilBuild = "232.*",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        )


    )

}


data class BuildProfile(
    val platformVersion: String,
    val riderVersion: String,
    val pycharmVersion: String,
    val riderResharperVersion: String,
    val riderResharperVersionConstant: String,
    val pythonPluginVersion: String,
    val platformVersionCode: String,
    val pluginSinceBuild: String,
    val pluginUntilBuild: String,
    val kotlinTarget: String,
    val javaVersion: String,
)