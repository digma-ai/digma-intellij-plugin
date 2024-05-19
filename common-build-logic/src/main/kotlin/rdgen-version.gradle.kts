import common.currentProfile
import common.rider.rdGenVersionByProfile

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.jetbrains.rd" && requested.name == "rd-gen") {
            val rdGenVersion = rdGenVersionByProfile(project.currentProfile().profile)
            useVersion(rdGenVersion)
            logger.lifecycle("Using rdgen library version $rdGenVersion")
            because("my rd gen version")
        }
    }
}