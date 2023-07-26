import common.platformVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    pluginName.set("system-test-plugin")
    version.set(platformVersion())
    type.set("IC")
    plugins.set(listOf("com.intellij.java","org.jetbrains.idea.maven","org.jetbrains.plugins.gradle"))

    pluginsRepositories {
        marketplace()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

dependencies {
    testImplementation("org.mockito:mockito-inline:3.6.28")
    implementation(project(":ide-common"))
    implementation(project(":java"))
    implementation(project(":model"))
    implementation(project(":"))
}

tasks.register<Copy>("copyTestResources") {
    from(sourceSets.main.get().resources.srcDirs)
    into("src/test/resources")
}

tasks.test {
    // Get current file directory.
    val testResourcesDir = File(project.projectDir, "src/main/resources")
    println("+++ test")
    println(testResourcesDir.absolutePath)
    // Based on testDir get the META_INF directory. The testDir is under system-test directory and META-INF directory is under META-INF directory of sibling src directory.
    val metaInfDir = File(project.projectDir.parentFile, "src/main/resources/META-INF")
    // Check if metaInfDir exists.
    if (!metaInfDir.exists()) {
        // Raise exception
        throw GradleException("META-INF directory not found in $metaInfDir")
    }
    // Copy plugin.xml inside META-INF directory to testDir, override if file already exists in testDir.
    val pluginXml = File(metaInfDir, "plugin.xml")
    if (!pluginXml.exists()) {
        // Raise exception
        throw GradleException("plugin.xml not found in $pluginXml")
    }
    pluginXml.copyTo(File(testResourcesDir, "plugin.xml"), overwrite = true)
    println(metaInfDir.absolutePath)
    println("+++ test")
//    useJUnit()
}
