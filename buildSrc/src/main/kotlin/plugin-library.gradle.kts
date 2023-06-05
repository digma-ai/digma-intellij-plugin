plugins {
    id("digma-base")
    id("java-library")
    id("org.jetbrains.intellij")
}

//todo: explore the plugin docs and make relevant changes
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

    runPluginVerifier {
        enabled = false
    }
    patchPluginXml{
        enabled = false
    }
}
