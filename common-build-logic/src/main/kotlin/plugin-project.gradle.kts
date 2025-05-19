import common.currentProfile
import common.logBuildProfile
import common.logIntellijPlatformPlugin
import common.properties
import common.withSilenceLogging
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("digma-base")
    id("common-java")
    id("common-kotlin")
    id("org.jetbrains.intellij.platform")
}


dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.JUnit5)
        if (project.currentProfile().isEAP) {
            //we need to supply a jetbrains runtime to runIde when using EAP because we use maven artifacts for EAP and not binary installers
            jetbrainsRuntime()
        }
    }
}

intellijPlatform {
    //buildSearchableOptions is a long-running task, we don't need it in every build,
    // local build can do without it. we activate it only in GitHub
    buildSearchableOptions = properties("buildSearchableOptions", project).toBoolean()
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

    //when building with buildPlugin the tests are not executed,
    //this dependency ensures they will.
    //it may increase build time and runIde time, but we want the tests to run on every buildPlugin.
    //Don't depend on build, it will cause circular dependency between tasks
    buildPlugin {
        dependsOn(test)
    }

    instrumentCode {
        instrumentationLogs = true
    }
}