plugins {
    id("plugin-library")
}

dependencies{
    compileOnly(project(":common"))
}

intellij {
    version.set("CL-2021.3.3")
}
