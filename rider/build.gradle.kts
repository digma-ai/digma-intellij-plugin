plugins {
    id("plugin-library")
}

dependencies{
    compileOnly(project(":common"))
}

intellij {
    version.set("RD-2021.3.3")
    plugins.set(listOf("rider-plugins-appender"))
}
