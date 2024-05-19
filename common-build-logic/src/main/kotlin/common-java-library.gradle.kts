//this plugin is for projects that are pure java, meaning they don't intellij platform in the compile classpath
//for example analytics-provider, model

plugins {
    id("digma-base")
    id("common-java")
    id("java-library")
}

tasks {
    //when the project is built with buildPlugin, modules that are
    //pure java will not be built or tested, this dependency ensures they will.
    //this is instead of specifically calling build in the command line
    //it may increase build time and runIde time, but we want the tests to run on every buildPlugin.
    create("buildPlugin").dependsOn(build)
}
