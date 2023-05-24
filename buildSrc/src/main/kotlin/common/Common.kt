package common

import org.gradle.api.Project

fun properties(key: String,project: Project) = project.findProperty(key).toString()

fun platformVersion(project: Project):String{
    return if (project.findProperty("useLatestVersion") == "true"){
        project.logger.lifecycle("building with latestPlatformVersion")
        properties("latestPlatformVersion",project)
    }else if (project.findProperty("useEAP") == "true"){
        project.logger.lifecycle("building with EAP")
        eapVersion(project)
    }
    else{
        project.logger.lifecycle("building with platformVersion")
        properties("platformVersion",project)
    }
}

private fun eapVersion(project: Project): String {
    return if (project.name == "rider"){
        properties("riderEAPVersion",project)
    }else{
        "LATEST-EAP-SNAPSHOT"
    }
}



fun pythonPluginVersion(project: Project):String{
    return if (project.findProperty("useLatestVersion") == "true"){
        project.logger.lifecycle("building with latestPythonPluginVersion")
        properties("latestPythonPluginVersion",project)
    }else if (project.findProperty("useEAP") == "true"){
        project.logger.lifecycle("building with EAP")
        properties("pythonPluginEAPVersion",project)
    }else{
        project.logger.lifecycle("building with pythonPluginVersion")
        properties("pythonPluginVersion",project)
    }
}



fun isWindows() = org.gradle.internal.os.OperatingSystem.current().isWindows
