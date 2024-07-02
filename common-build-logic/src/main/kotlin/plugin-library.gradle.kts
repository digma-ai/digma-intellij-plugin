import common.logBuildProfile
import common.logIntellijPlatformPlugin
import common.withSilenceLogging

plugins {
    id("digma-base")
    id("common-java")
    id("common-kotlin")
    id("java-library")
    id("org.jetbrains.intellij.platform.module")
}


repositories {
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

dependencies {
    intellijPlatform {
        instrumentationTools()
        pluginVerifier()
        //we need to supply a jetbrains runtime to runIde because we use maven artifacts for IDEs
        jetbrainsRuntime()
    }
}



afterEvaluate {
    if (gradle.startParameter.taskNames.contains("buildPlugin")) {
        withSilenceLogging {
            logBuildProfile(project)
            logIntellijPlatformPlugin(project, intellijPlatform)
        }
    }
}


tasks {

    //when the project is built with buildPlugin, modules that are plugin modules are not
    //tested because intellij platform gradle plugin only executes some tasks for these modules,
    //tasks that are necessary for compiling and packaging.
    //this dependency ensures that also the test task will be executed for these modules.
    //this is instead of specifically calling build or test in the command line.
    //it may increase build time and runIde time, but we want the tests to run on every buildPlugin.
    create("buildPlugin").dependsOn(build)

    instrumentCode {
        instrumentationLogs = true
    }
}
