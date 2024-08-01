import common.buildVersion
import common.properties

plugins {
    id("java")
    id("de.undercouch.download")
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
            if (requested.group == "org.jetbrains.kotlin" &&
                (requested.name == "kotlin-stdlib-jdk8" || requested.name == "kotlin-stdlib-jdk7")
            ) {
                useVersion("1.7.0")
                because(
                    "we should stick to version 1.7.0 because this is the version used in 2022.3 which is the lowest version we support. " +
                            "should never compile with higher version"
                )
            }
        }
    }
}



tasks {
    register("allDependencies") {
        dependsOn("dependencies")
    }
}