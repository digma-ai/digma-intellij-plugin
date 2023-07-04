package common

import org.gradle.api.Project

fun properties(key: String,project: Project) = project.findProperty(key).toString()


fun logBuildProfile(project: Project) {
    project.logger.lifecycle("platformType for project ${project.name} is ${properties("platformType", project)}")
    project.logger.lifecycle(
        "Building ${project.name}: " +
                "platformVersion: ${project.platformVersion()}, " +
                "buildVersion: ${project.buildVersion()}, " +
                "platformPlugins: ${project.platformPlugins()}, " +
                "buildProfile: ${project.currentProfile()}"
    )
}


fun isWindows() = org.gradle.internal.os.OperatingSystem.current().isWindows
