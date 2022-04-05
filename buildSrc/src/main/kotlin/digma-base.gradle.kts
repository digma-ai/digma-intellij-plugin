plugins{
    `java`
}

fun properties(key: String) = project.findProperty(key).toString()


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(properties("javaVersion")))
//        vendor.set(JvmVendorSpec.AMAZON)
    }
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
    mavenCentral()
}

configurations {
    all {
        // Allows using project dependencies instead of IDE dependencies during compilation and test running
        resolutionStrategy {
            sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
        }
    }
}