import common.buildVersion
import common.properties

plugins {
    id("java")
}


group = properties("pluginGroup", project)
version = project.buildVersion()




repositories {
    mavenCentral()
    // jetbrains artifacts repositories
    maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-repository/snapshots")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

configurations {
    all {
        // Allows using project dependencies instead of IDE dependencies during compilation and test running
        resolutionStrategy {
            sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
        }
    }
}


project.afterEvaluate {
    tasks {
        //the project can be built with build task or buildPlugin task. build triggers buildPlugin.
        //but buildPlugin doesn't trigger build, so if calling only buildPlugin unit tests will not run.
        //if the project is built by calling buildPlugin we want it to trigger test.
        named("buildPlugin") {
            dependsOn(test)
        }
    }
}


