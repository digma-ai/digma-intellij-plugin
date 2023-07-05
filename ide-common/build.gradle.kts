import common.dynamicPlatformType
import common.logBuildProfile
import common.platformVersion

plugins {
    id("plugin-library")
}

//ide-common module should build with different platform types, if running rider with runIde it should
// build with RD, if running idea it should build with IC, etc.
val platformType by extra(dynamicPlatformType(project))

logBuildProfile(project)

intellij {
    version.set("$platformType-${project.platformVersion()}")
    plugins.set(listOf("Git4Idea"))
}

dependencies {
    //pretty time can be moved to the model project to it s accessible to all project classes.
    //from here the model classes can't use it
    api(libs.prettytime)
    api(libs.threeten)
    api(libs.commons.lang3)
    api(libs.commons.collections4)
    api(libs.posthog)
    api(libs.maven.artifact)

    implementation(project(":model"))
    implementation(project(":analytics-provider"))
}

