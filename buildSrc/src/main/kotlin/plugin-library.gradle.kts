plugins {
    id("digma-base")
    `java-library`
    id("org.jetbrains.intellij")
}

tasks{
    buildSearchableOptions {
        enabled = false
    }

    runIde{
        enabled = false
    }
}
