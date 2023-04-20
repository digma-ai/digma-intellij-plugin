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

    implementation(project(":model"))
    implementation(project(":analytics-provider"))
    implementation("com.posthog.java:posthog:+")
}

tasks{

    task("injectPosthogTokenUrl") {
        doLast{
            val url = System.getenv("POSTHOG_TOKEN_URL") ?: ""
            file("../build/resources/main/posthog-token-url.txt").writeText(url)
        }
    }
    buildPlugin {
        dependsOn("injectPosthogTokenUrl")
    }
}