//this plugin is for projects that are pure java, meaning they don't have the
//org.jetbrains.intellij plugin.
//for example analytics-provider, model

plugins {
    id("digma-base")
    id("java-library")
}


tasks {
    //a workaround: if the project is built by calling buildPlugin
    // then build is not called for projects that are not intellij plugin project.
    // for example the :model project and :analytics-provider
    create("buildPlugin").dependsOn(build)
}
