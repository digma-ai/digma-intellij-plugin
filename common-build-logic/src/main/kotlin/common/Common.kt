package common

import org.gradle.api.Project
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension

//send -Pdigma-no-info-logging to silence some console logging, useful when debugging issues so the log is not too noisy.
const val DIGMA_NO_INFO_LOGGING = "digma-no-info-logging"

//use for messages we want to silence sometimes with DIGMA_NO_INFO_LOGGING
fun Project.withSilenceLogging(consumer: () -> Unit){
    if (!hasProperty(DIGMA_NO_INFO_LOGGING)) {
        consumer.invoke()
    }
}



fun properties(key: String, project: Project) = project.findProperty(key).toString()

fun platformTypeProperty(project: Project) = project.findProperty("platformType") as IntelliJPlatformType


fun logBuildProfile(project: Project) {
    project.logger.lifecycle("")
    project.logger.lifecycle("############ Build Profile for project ${project.name}  ####################")
    project.logger.lifecycle("Build profile for project ${project.name} is '${project.currentProfile().profile}' [${project.currentProfile()}]")
    project.logger.lifecycle("platformType for project ${project.name} is ${platformTypeProperty(project)}")
    project.logger.lifecycle("buildVersion for project ${project.name} is ${project.buildVersion()}")
    project.logger.lifecycle("############################################################################")
    project.logger.lifecycle("")
    project.logger.lifecycle("")
    project.logger.lifecycle("")
}


fun logIntellijPlatformPlugin(project: Project, intellijPlatform: IntelliJPlatformExtension) {
    project.logger.lifecycle("")
    project.logger.lifecycle("##########  Intellij platform plugin info for project ${project.name} ######")
    project.logger.lifecycle("platformPath ${intellijPlatform.platformPath}")
    project.logger.lifecycle("productInfo.name ${intellijPlatform.productInfo.name}")
    project.logger.lifecycle("productInfo.version ${intellijPlatform.productInfo.version}")
    project.logger.lifecycle("productInfo.buildNumber ${intellijPlatform.productInfo.buildNumber}")
    project.logger.lifecycle("productInfo.productCode ${intellijPlatform.productInfo.productCode}")
    project.logger.lifecycle("productInfo.bundledPlugins ${intellijPlatform.productInfo.bundledPlugins}")
    project.logger.lifecycle("productInfo.modules ${intellijPlatform.productInfo.modules}")
    project.logger.lifecycle("##############################################################################")
    project.logger.lifecycle("")
    project.logger.lifecycle("")
    project.logger.lifecycle("")
}


fun isWindows() = org.gradle.internal.os.OperatingSystem.current().isWindows
