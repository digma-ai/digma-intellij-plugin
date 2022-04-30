import common.properties

plugins {
    id("plugin-library")
}

dependencies{
    compileOnly(project(":ide-common"))
}

intellij {
    version.set("PC-"+ properties("platformVersion", project))
    plugins.set(listOf("PythonCore"))
}
