import common.buildVersion
import common.properties

plugins {
    id("java")
}


group = properties("pluginGroup", project)
version = project.buildVersion()

configurations {
    all {
        // Allows using project dependencies instead of IDE dependencies during compilation and test running
        resolutionStrategy {
            sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
        }

        resolutionStrategy.eachDependency {
            if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-databind" && requested.version == "2.10.5") {
                //todo: it we upgrade jackson to a version higher then 2.10.5 then this is not necessary anymore
                useVersion("2.10.5.1")
                because("https://devhub.checkmarx.com/cve-details/CVE-2021-20190/?utm_source=jetbrains&utm_medium=referral&utm_campaign=idea")
            }
        }
    }
}

dependencies{
    //adding junit 4 is a workaround for this:
    //https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
    //https://youtrack.jetbrains.com/issue/IJPL-159134/JUnit5-Test-Framework-refers-to-JUnit4-java.lang.NoClassDefFoundError-junit-framework-TestCase
    //todo: check in future versions of the platform if it can be removed
    testRuntimeOnly("junit:junit:4.13.2")
}

tasks {
    register("allDependencies") {
        dependsOn("dependencies")
    }
}