import common.rider.rdGenVersion

pluginManagement {

    resolutionStrategy {
        eachPlugin {
            // Gradle has to map a plugin dependency to Maven coordinates - '{groupId}:{artifactId}:{version}'. It tries
            // to do use '{plugin.id}:{plugin.id}.gradle.plugin:version'.
            // This doesn't work for rdgen, so we provide some help
            if (requested.id.id == "com.jetbrains.rdgen") {

                val rdGenVersion = if (settings.providers.gradleProperty("buildProfile").isPresent) {
                    val profile = settings.providers.gradleProperty("buildProfile").get()
                    rdGenVersion(profile)
                } else {
                    requested.version
                }

                logger.lifecycle("Using rdgen plugin version $rdGenVersion")
                useVersion(rdGenVersion)
                useModule("com.jetbrains.rd:rd-gen:${rdGenVersion}")
            }
        }
    }
}