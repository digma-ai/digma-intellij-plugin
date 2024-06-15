import common.logBuildProfile
import common.logIntellijPlatformPlugin
import common.properties

plugins {
    id("digma-base")
    id("common-java")
    id("common-kotlin")
    id("org.jetbrains.intellij.platform")
}


repositories {
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

intellijPlatform {
    //buildSearchableOptions is a long-running task, we don't need it in every build,
    // local build can do without it. we activate it only in GitHub
    buildSearchableOptions = properties("buildSearchableOptions", project).toBoolean()
}

afterEvaluate {
    if (gradle.startParameter.taskNames.contains("buildPlugin")) {
        logBuildProfile(project)
        logIntellijPlatformPlugin(project, intellijPlatform)
    }
}


dependencies {
    intellijPlatform {
        instrumentationTools()
    }
}

tasks {

    //when building with buildPlugin the tests are not executed,
    //this dependency ensures they will.
    //it may increase build time and runIde time, but we want the tests to run on every buildPlugin.
    buildPlugin {
        dependsOn(build)
    }

    instrumentCode {
        instrumentationLogs = true
    }
}