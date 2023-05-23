import common.platformVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    version.set("IC-"+ platformVersion(project))
    plugins.set(listOf("com.intellij.java","org.jetbrains.idea.maven","org.jetbrains.plugins.gradle"))
}

dependencies {
    testImplementation(project(":ide-common"))
    testImplementation(project(":java"))
    testImplementation(project(":model"))
}

tasks.test {
    useJUnit()
}
