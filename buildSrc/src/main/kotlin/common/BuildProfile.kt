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
        IdeFlavor.IC.name, IdeFlavor.IU.name -> "com.intellij.java,org.jetbrains.kotlin" //not really required because both are built in plugins

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
    return findProperty("doNotDownloadSources") == null
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

    enum class Profiles { p223, p231, p232, p233, p241, p242 }

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
        "latest" to Profiles.p233.name,
        "eap" to Profiles.p242.name,
    )


    //jetbrains repositories:
    //https://www.jetbrains.com/intellij-repository/snapshots
    //https://www.jetbrains.com/intellij-repository/releases
    //https://plugins.jetbrains.com/plugin/7322-python-community-edition/versions/stable
    //./gradlew clean buildPlugin -PbuildProfile=p233

    //todo: check products releases file, can be used to automatically update the profiles
    // https://www.jetbrains.com/updates/updates.xml
    // IntelliJPluginConstants.IDEA_PRODUCTS_RELEASES_URL
    // https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-listproductsreleases

    private val profiles = mapOf(

        Profiles.p223 to BuildProfile(
            profile = Profiles.p223,
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
            platformVersion = "2023.1.6",
            riderVersion = "2023.1.4",
            pycharmVersion = "2023.1.5",
            riderResharperVersion = "2023.1.4",
            riderResharperVersionConstant = "PROFILE_2023_1",
            pythonPluginVersion = "231.9225.4",
            platformVersionCode = "231",
            pluginSinceBuild = "231",
            pluginUntilBuild = "231.*",
            versionToRunPluginVerifier = "2023.1.5",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),

        Profiles.p232 to BuildProfile(
            profile = Profiles.p232,
            platformVersion = "2023.2.6",
            riderVersion = "2023.2.3",
            pycharmVersion = "2023.2.6",
            riderResharperVersion = "2023.2.3",
            riderResharperVersionConstant = "PROFILE_2023_2",
            pythonPluginVersion = "232.9559.62",
            platformVersionCode = "232",
            pluginSinceBuild = "232",
            pluginUntilBuild = "232.*",
            versionToRunPluginVerifier = "2023.2.5",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),


        Profiles.p233 to BuildProfile(

            profile = Profiles.p233,
            platformVersion = "2023.3.4",
            riderVersion = "2023.3.3",
            pycharmVersion = "2023.3.3",
            riderResharperVersion = "2023.3.3",
            riderResharperVersionConstant = "PROFILE_2023_2",
            pythonPluginVersion = "233.14015.106",
            platformVersionCode = "233",
            pluginSinceBuild = "233",
            pluginUntilBuild = "233.*",
            versionToRunPluginVerifier = "2023.3.3",
            kotlinTarget = KotlinVersion.KOTLIN_1_9.version,
            javaVersion = JavaVersion.VERSION_17.majorVersion
        ),


        Profiles.p241 to BuildProfile(

            profile = Profiles.p241,
            platformVersion = "2024.1",
            riderVersion = "2024.1",
            pycharmVersion = "2024.1",
            riderResharperVersion = "2024.1.0",
            riderResharperVersionConstant = "PROFILE_2023_2",
            pythonPluginVersion = "241.14494.240",
            platformVersionCode = "241",
            pluginSinceBuild = "241",
            pluginUntilBuild = "241.*",
            versionToRunPluginVerifier = "2024.1",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version, //todo: maybe need to upgrade to 20
            javaVersion = JavaVersion.VERSION_17.majorVersion,
        ),

        //EAP
        //todo: currently same as 241, update to EAP when started, create launchers.
        // add to github matrix
        Profiles.p242 to BuildProfile(

            isEAP = true,
            profile = Profiles.p242,
            platformVersion = "2024.1",
            riderVersion = "2024.1",
            pycharmVersion = "2024.1",
            riderResharperVersion = "2024.1.0",
            riderResharperVersionConstant = "PROFILE_2023_2",
            pythonPluginVersion = "241.14494.240",
            platformVersionCode = "241",
            pluginSinceBuild = "241",
            pluginUntilBuild = "241.*",
            versionToRunPluginVerifier = "2024.1",
            kotlinTarget = KotlinVersion.KOTLIN_1_8.version, //todo: maybe need to upgrade to 20
            javaVersion = JavaVersion.VERSION_17.majorVersion,
        )

    )

}

/*
 * Notes:
 * pythonPluginVersion:
 *  pythonPluginVersion is necessary when building the python module with type=IC, in that case the version needs to be compatible
 *  with the IC platform version. if building the python module with type=PC then pythonPluginVersion is not relevant.
 *  building with type=PC means more disk space in github.
 *  currently we build python with type=PC because matching the pythonPluginVersion and latest EAP build is not always possible,
 *  sometimes it takes time before there is a compatible python plugin version. and anyway it's easier to just build with PC.
 *
 *
 *
 */
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