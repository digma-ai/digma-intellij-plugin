import common.platformVersion
import org.gradle.initialization.ClassLoaderScopeRegistryListenerManager
import org.jetbrains.kotlin.com.intellij.openapi.util.registry.Registry

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    pluginName.set("system-test-plugin")
    version.set(platformVersion())
    type.set("IC")
    plugins.set(listOf("com.intellij.java", "org.jetbrains.idea.maven", "org.jetbrains.plugins.gradle"))

    pluginsRepositories {
        marketplace()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

dependencies {
    implementation("org.mockito:mockito-core:5.4.0")
    implementation("org.mockito:mockito-inline:5.2.0")
    implementation(project(":ide-common"))
    implementation(project(":analytics-provider"))
    implementation(project(":java"))
    implementation(project(":rider"))
    implementation(project(":model"))
    implementation(project(":"))
}

tasks.test {
    systemProperty("intellij.progress.task.ignoreHeadless", true)
    useJUnit()
}
