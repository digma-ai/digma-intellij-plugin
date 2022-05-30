import common.properties

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    version.set("IC-" + properties("platformVersion", project))
}

dependencies {
    implementation("org.ocpsoft.prettytime:prettytime:5.0.3.Final")

    implementation(project(":model"))
    implementation(project(":analytics-provider"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
