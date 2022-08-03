import common.properties

plugins{
    `java`
    id("com.dorongold.task-tree")
    id("com.glovoapp.semantic-versioning")
}

semanticVersion{
    //if the propertiesFile is not changed the plugin will look for a file in each module.
    propertiesFile.set(project.rootProject.file("version.properties"))
}
tasks.incrementSemanticVersion{
    //disable the task for all projects.
    //because digma-base is applied to all projects then calling incrementSemanticVersion will be invoked
    //for each project and we don't want that.
    //we could apply the plugin only to the main project but then we can't use its tasks in script plugins.
    //the task is enabled only in the main project.
    enabled = false
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(properties("javaVersion",project)))
        //to build with amazon corretto download it in the gradlew script. see resharper-unity
        //vendor.set(JvmVendorSpec.AMAZON)
    }
}

group = properties("pluginGroup",project)
version = project.semanticVersion.version.get()

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