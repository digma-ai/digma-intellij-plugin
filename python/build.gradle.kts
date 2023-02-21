import common.properties

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
    //here we can depend on pycharm. but then pycharm needs to download. in github the build may fail with
    // no space left on device. so it's possible to depend on IC with python plugin instead because IC is downloaded anyway..
    version.set("IC-" + properties("platformVersion", project))
    //the python plugin version must be compatible with platformVersion
    plugins.set(listOf("PythonCore:223.7571.182"))

//    version.set("PY-" + properties("platformVersion", project))
//    plugins.set(listOf("PythonCore:223.7571.182"))
    downloadSources.set(false)
//    version.set("PY-" + properties("platformVersion", project))
//    plugins.set(listOf("Pythonid"))
}
