plugins {
    id("plugin-library")
}

dependencies{
    compileOnly(project(":common"))
}

intellij {
    version.set("PC-2021.3.3")
    plugins.set(listOf("PythonCore"))
}
