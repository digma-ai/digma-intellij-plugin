import common.properties

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    version.set("IC-"+ properties("platformVersion", project))
}

dependencies {
    implementation(project(":model"))
    implementation(project(":analytics-provider"))
}
