import common.properties

plugins {
    id("plugin-library")
}

dependencies{
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))
}

intellij {
    version.set("PC-"+ properties("platformVersion", project))
    plugins.set(listOf("PythonCore"))
}
