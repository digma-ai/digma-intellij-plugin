import common.IdeFlavor
import common.logBuildProfile
import common.platformVersion
import common.shouldDownloadSources

plugins {
    id("plugin-library")
}


dependencies {
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))
}

//java module should always build with IC
val platformType by extra(IdeFlavor.IC.name)

logBuildProfile(project)

intellij {
    version.set("$platformType-${project.platformVersion()}")
    plugins.set(listOf("com.intellij.java", "org.jetbrains.idea.maven", "org.jetbrains.plugins.gradle"))
    downloadSources.set(project.shouldDownloadSources())
}
