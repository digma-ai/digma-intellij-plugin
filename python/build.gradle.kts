import common.properties

plugins {
    id("plugin-library")
}

dependencies{
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))
}

intellij {
    version.set("IC-" + properties("platformVersion", project))
    //the python plugin version must be compatible with platformVersion
    plugins.set(listOf("PythonCore:223.7571.182"))
}
