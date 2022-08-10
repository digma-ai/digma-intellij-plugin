import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

//this plugin is for projects that are pure java, meaning they don't have the
//org.jetbrains.intellij plugin.
//for example analytics-provider, model

plugins {
    id("digma-base")
    `java-library`
}


tasks {
    //a workaround: if the project is built by calling buildPlugin
    // then build is not called for projects that are not intellij plugin project.
    create("buildPlugin").dependsOn(build)
}
