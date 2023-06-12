import common.platformVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}


dependencies{
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))
}

//java module should always build with IC
val platformType by extra("IC")

intellij {
    version.set("IC-"+ platformVersion(project))
    plugins.set(listOf("com.intellij.java","org.jetbrains.idea.maven","org.jetbrains.plugins.gradle"))
}
