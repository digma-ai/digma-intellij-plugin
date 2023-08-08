import common.platformVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    pluginName.set("system-test-plugin")
    version.set(platformVersion(project))
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

tasks.test {
    systemProperty("intellij.progress.task.ignoreHeadless", true)
    useJUnit()
}
