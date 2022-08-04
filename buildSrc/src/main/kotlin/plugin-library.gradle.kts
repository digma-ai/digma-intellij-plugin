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
        //if runIde in those modules is enabled then runIde for the plugin project will run them all
        enabled = false
    }

    jarSearchableOptions{
        enabled = false
    }

    runPluginVerifier {
        enabled = false
    }

    verifyPlugin{
        enabled = false
    }

    signPlugin {
        enabled = false
    }

    publishPlugin {
        enabled = false
    }

    listProductsReleases{
        enabled = false
    }

    patchPluginXml{
        enabled = false
    }
}
