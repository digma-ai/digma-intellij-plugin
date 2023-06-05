package common

import org.gradle.api.Project

fun properties(key: String,project: Project) = project.findProperty(key).toString()

fun platformVersion(project: Project):String{

    val platformType = properties("platformType",project)
    val projectName = project.name

    return if (project.findProperty("useLatestVersion") == "true"){
        project.logger.lifecycle("building with latestPlatformVersion")
        getForLatestReleaseVersion(project,projectName,platformType)
    }else if (project.findProperty("useEAPVersion") == "true"){
        project.logger.lifecycle("building with EAP")
        getForEAPVersion(project,projectName,platformType)
    }else{
        val platforVersion = properties("platformVersion",project)
        project.logger.lifecycle("building with $platforVersion")
        platforVersion
    }
}




private fun getForLatestReleaseVersion(project: Project, projectName: String, platformType: String):String{
    return if (projectName == "rider" || platformType == "RD"){
        val riderVersion = properties("latestRiderVersion",project)
        project.logger.lifecycle("building rider with $riderVersion")
        riderVersion
    }else if(platformType == "PC" || platformType == "PY"){
        //this is for launchers with platformType=PC or PY
        //assuming that PC and PY are always the same version
        val pycharmVersion = properties("latestPycharmVersion",project)
        project.logger.lifecycle("building with pycharm  $pycharmVersion")
        pycharmVersion
    }else{
        //this will handle the java module and python module because python module uses IC.
        //will also handle launchers with platformType=IC or platformType=IU assuming IC and IU are always the same version
        val latestPlatformVersion = properties("latestPlatformVersion",project)
        project.logger.lifecycle("building with $latestPlatformVersion")
        latestPlatformVersion
    }
}

private fun getForEAPVersion(project: Project, projectName: String, platformType: String):String{
    return if (projectName == "rider" || platformType == "RD"){
        val riderEapVersion = properties("eapRiderVersion",project)
        project.logger.lifecycle("building rider with $riderEapVersion")
        riderEapVersion
    }else if(platformType == "PC" || platformType == "PY"){
        //this is for launchers with platformType=PC or PY
        //assuming that PC and PY are always the same version
        val pycharmEapVersion = properties("eapPycharmVersion",project)
        project.logger.lifecycle("building with pycharm  $pycharmEapVersion")
        pycharmEapVersion
    }else{
        //this will handle the java module and python module because python module uses IC.
        //will also handle launchers with platformType=IC or platformType=IU assuming IC and IU are always the same version
        val latestEapPlatformVersion = properties("eapPlatformVersion",project)
        project.logger.lifecycle("building with $latestEapPlatformVersion")
        latestEapPlatformVersion
    }
}




fun pythonPluginVersion(project: Project):String{
    return if (project.findProperty("useLatestVersion") == "true"){
        val latestPythonPluginVersion = properties("latestPythonPluginVersion",project)
        project.logger.lifecycle("building with python $latestPythonPluginVersion")
        latestPythonPluginVersion
    }else if (project.findProperty("useEAPVersion") == "true"){
        val eapPythonPluginVersion = properties("eapPythonPluginVersion",project)
        project.logger.lifecycle("building with python $eapPythonPluginVersion")
        eapPythonPluginVersion
    }else{
        val pythonPluginVersion = properties("pythonPluginVersion",project)
        project.logger.lifecycle("building with python $pythonPluginVersion")
        pythonPluginVersion
    }
}



fun isWindows() = org.gradle.internal.os.OperatingSystem.current().isWindows
