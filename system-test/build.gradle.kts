import common.platformVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}

//intellij {
//    version.set("IC-"+ platformVersion(project))
//    plugins.set(listOf("com.intellij.java","org.jetbrains.idea.maven","org.jetbrains.plugins.gradle"))
//}
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
    implementation(project(":ide-common"))
    implementation(project(":java"))
    implementation(project(":model"))
}

tasks.test {
    useJUnit()
}

