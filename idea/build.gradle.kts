plugins {
    id("plugin-library")
}


dependencies{
    compileOnly(project(":common"))
}

intellij {
    version.set("IC-2021.3.3")
    plugins.set(listOf("com.intellij.java"))
}
