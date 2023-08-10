import common.IdeFlavor
import common.currentProfile
import common.logBuildProfile
import common.platformVersion

plugins {
    id("plugin-library")
}

dependencies {
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))

    testImplementation(project(":model"))
}

//python module should always build with IC or PC
val platformType by extra(IdeFlavor.IC.name)

logBuildProfile(project)


intellij {

    //there is no source code for pycharm or python plugin
    downloadSources.set(false)

    //there are two ways to build the python module:

    //with Idea and python plugin, python plugin version must be specified.
    //the python plugin version must be compatible with platformVersion
//    platformType = IC
//    version.set("$platformType-${project.platformVersion()}")
//    plugins.set(listOf("PythonCore:${project.currentProfile().pythonPluginVersion}"))

    //or with pycharm, python plugin version does not need to be specified.
//   platformType = PC
//   version.set("$platformType-${project.currentProfile().pycharmVersion}")
//   plugins.set(listOf("PythonCore"))

    version.set("$platformType-${project.platformVersion()}")
    plugins.set(listOf("PythonCore:${project.currentProfile().pythonPluginVersion}"))

}
