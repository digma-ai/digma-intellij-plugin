import common.IdeFlavor
import common.logBuildProfile
import common.platformVersion

plugins {
    id("plugin-library")
}


dependencies {
    compileOnly(project(":ide-common"))
    compileOnly(project(":jvm-common"))
}

//jvm module should always build with IC
val platformType by extra(IdeFlavor.IC.name)

logBuildProfile(project)

intellij {
    version.set("$platformType-${project.platformVersion()}")
    plugins.set(listOf("com.intellij.java", "org.jetbrains.plugins.gradle"))
}
