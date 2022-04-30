import common.properties

plugins{
    `java`
    id("com.dorongold.task-tree")
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(properties("javaVersion",project)))
//        vendor.set(JvmVendorSpec.AMAZON)
    }
}

group = properties("pluginGroup",project)
version = properties("pluginVersion",project)

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