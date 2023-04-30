import common.platformVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    version.set("IC-" + platformVersion(project))
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

    implementation(project(":model"))
    implementation(project(":analytics-provider"))
}

tasks{

    val injectPosthogTokenUrlTask = task("injectPosthogTokenUrl") {
        doLast{
            val url = System.getenv("POSTHOG_TOKEN_URL") ?: ""
            file("${project.rootProject.sourceSets.main.get().output.resourcesDir?.absolutePath}/posthog-token-url.txt").writeText(url)
        }
    }

    processResources.get().finalizedBy(injectPosthogTokenUrlTask)
}