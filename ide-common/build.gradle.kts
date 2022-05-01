plugins {
    id("plugin-library")
}

intellij {
    version.set("IC-2021.3.3")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":analytics-provider"))
}
