import common.platformVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    pluginName.set("system-test-plugin")
    version.set(platformVersion())
    type.set("IC")
    plugins.set(listOf("com.intellij.java", "org.jetbrains.kotlin", "org.jetbrains.idea.maven", "org.jetbrains.plugins.gradle"))

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
    implementation(project(":jvm-common"))
    implementation(project(":rider"))
    implementation(project(":model"))
    implementation(project(":"))
}

tasks.test {
    // By default, the property is set to false and the headless mode runs EDT task on the main thread in a synchronous way.
    // this insures that the async tasks of the IDE run in an asynchronous way. 
    systemProperty("intellij.progress.task.ignoreHeadless", true)
    useJUnit()
}
