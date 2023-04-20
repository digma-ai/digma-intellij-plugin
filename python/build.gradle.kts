import common.platformVersion
import common.pythonPluginVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}

dependencies{
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))

    testImplementation(project(":model"))
}

intellij {

    //there is no source code for pycharm or python plugin
    downloadSources.set(false)

    //the best would be to depend on pycharm. but github build fails with no space left on device especially if running
    //plugin verifier with PC.
    //So it is possible not to depend on pycharm but to depend on IC with PythonCore plugin. that means we are limited
    //to what PythonCore provides. if we ever need specific pycharm functionality we will need to depend on pycharm.

    version.set("IC-" + platformVersion(project))
    //the python plugin version must be compatible with platformVersion
    plugins.set(listOf("PythonCore:" + pythonPluginVersion(project)))

    //to depend on pycharm community:
//    version.set("PC-" + platformVersion(project))
//    plugins.set(listOf("PythonCore:223.7571.182"))
    //to depend on pycharm professional:
//    version.set("PY-" + platformVersion(project))
//    plugins.set(listOf("Pythonid"))
}
