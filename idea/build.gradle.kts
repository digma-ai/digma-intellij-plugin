import common.properties

plugins {
    id("plugin-library")
}


dependencies{
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))
}

intellij {
    version.set("IC-"+ properties("platformVersion",project))
    plugins.set(listOf("com.intellij.java"))
}
