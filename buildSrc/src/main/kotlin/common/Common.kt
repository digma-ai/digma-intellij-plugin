package common

import org.gradle.api.Project

fun properties(key: String,project: Project) = project.findProperty(key).toString()

fun platformVersion(project: Project):String{
    return if (project.findProperty("useLatestVersion") == "true"){
        project.logger.lifecycle("building with latestPlatformVersion")
        properties("latestPlatformVersion",project)
    }else{
        project.logger.lifecycle("building with platformVersion")
        properties("platformVersion",project)
    }
}

fun pythonPluginVersion(project: Project):String{
    return if (project.findProperty("useLatestVersion") == "true"){
        project.logger.lifecycle("building with latestPythonPluginVersion")
        properties("latestPythonPluginVersion",project)
    }else{
        project.logger.lifecycle("building with pythonPluginVersion")
        properties("pythonPluginVersion",project)
    }
}



fun isWindows() = org.gradle.internal.os.OperatingSystem.current().isWindows
