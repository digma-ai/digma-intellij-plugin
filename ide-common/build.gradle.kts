import common.properties

plugins {
    id("plugin-library")
    id("common-kotlin")
}

intellij {
    version.set("IC-" + properties("platformVersion", project))
    plugins.set(listOf("Git4Idea"))
}

dependencies {
    //pretty time can be moved to the model project to it s accessible to all project classes.
    //from here the model classes can't use it
    api(libs.prettytime)
    api(libs.threeten)


    implementation(project(":model"))
    implementation(project(":analytics-provider"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
