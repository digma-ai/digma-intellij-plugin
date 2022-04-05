// Affects the repositories used to resolve the plugins { } block
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { setUrl("https://cache-redirector.jetbrains.com/plugins.gradle.org") }
    }
}
