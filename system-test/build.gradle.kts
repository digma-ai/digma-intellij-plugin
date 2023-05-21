import common.platformVersion

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    version.set("IC-" + platformVersion(project))
}

dependencies {
    testImplementation(project(":ide-common"))
    testImplementation(project(":java"))
    testImplementation(project(":model"))
}

tasks.test {
    useJUnit()
}
