plugins {
    id("digma-base")
    id("common-java")
    id("common-kotlin")
    id("java-library")
    id("org.jetbrains.intellij")
}

//this plugin is applied to modules that are compiled with intellij platform
// but are not plugins and don't need to run many of gradle-intellij-plugin
// tasks. the unnecessary tasks are disabled.
//we have only one real plugin and its the root project that really builds a plugin.
tasks{

    listProductsReleases {
        enabled = false
    }

    buildSearchableOptions {
        enabled = false
    }

    downloadRobotServerPlugin{
        enabled = false
    }

    runIde{
        //if runIde in those modules is enabled then runIde for the plugin project will run them all
        enabled = false
    }

    runIdeForUiTests{
        enabled = false
    }

    runIdePerformanceTest{
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
