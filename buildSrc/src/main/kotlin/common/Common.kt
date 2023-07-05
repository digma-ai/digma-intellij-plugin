package common

import org.gradle.api.Project

fun properties(key: String,project: Project) = project.findProperty(key).toString()


fun logBuildProfile(project: Project) {

    project.logger.lifecycle("################################################################")

    project.logger.lifecycle("Build profile for project ${project.name} is '${project.currentProfile().profile}'")

    project.logger.lifecycle("platformType for project ${project.name} is ${properties("platformType", project)}")
    project.logger.lifecycle(
        "Building ${project.name}: " +
                "platformVersion: ${project.platformVersion()}, " +
                "buildVersion: ${project.buildVersion()}, " +
                "platformPlugins: ${project.platformPlugins()}, " +
                "buildProfile: ${project.currentProfile()}"
    )

    project.logger.lifecycle("################################################################")
    project.logger.lifecycle("")
}


fun isWindows() = org.gradle.internal.os.OperatingSystem.current().isWindows
