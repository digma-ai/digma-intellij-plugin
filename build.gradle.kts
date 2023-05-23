import common.platformVersion
import common.properties
import org.jetbrains.changelog.date
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.tasks.ListProductsReleasesTask
import org.jetbrains.changelog.exceptions.MissingVersionException
import java.util.EnumSet

fun properties(key: String) = properties(key,project)

@Suppress(
    //see: https://youtrack.jetbrains.com/issue/KTIJ-19369
    "DSL_SCOPE_VIOLATION"
)
plugins {
    id("plugin-project")
    id("org.jetbrains.changelog") version "2.0.0"
    id("org.jetbrains.qodana") version "0.1.13"
    id("common-kotlin")
}




dependencies{
    implementation(libs.commons.lang3)
    implementation(project(":model"))
    implementation(project(":analytics-provider"))
    implementation(project(":ide-common"))
    implementation(project(":java"))
    implementation(project(":python"))
//    implementation(project(":rider"))
    implementation(project(":system-test"))
    implementation(libs.freemarker)
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(platformVersion(project))
    type.set(properties("platformType"))
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))

    pluginsRepositories {
        marketplace()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(project.semanticVersion.version.get().toString())
    path.set("${project.projectDir}/CHANGELOG.md")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
    header.set(provider { "[${version.get()}] - ${date()}" })
    keepUnreleasedSection.set(false)
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(projectDir.resolve(".qodana").canonicalPath)
    reportPath.set(projectDir.resolve("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}


project.afterEvaluate{
    //the final plugin distribution is packaged from the sandbox.
    //So,make all the subprojects buildPlugin task run before this project's buildPlugin.
    //that will make sure that their prepareSandbox task runs before building the plugin coz
    //maybe they contribute something to the sandbox.
    //currently, only rider contributes the dotnet dll's to the sandbox.

    //it can be written with task fqn like buildPlugin.dependsOn(":rider:buildPlugin")
    //but this syntax is not favorite by the gradle developers becasue it will cause eager initialization of the task.
    val buildPlugin = tasks.named("buildPlugin").get()
    project(":java").afterEvaluate { buildPlugin.dependsOn(tasks.getByName("buildPlugin")) }
    project(":python").afterEvaluate { buildPlugin.dependsOn(tasks.getByName("buildPlugin")) }
//    project(":rider").afterEvaluate { buildPlugin.dependsOn(tasks.getByName("buildPlugin")) }
}



tasks {

    incrementSemanticVersion {
        //enable the SemanticVersion only for this module
        enabled = true
    }

    jar {
//        dependsOn(":rider:copyKotlinModuleFile")
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
        distributionType = Wrapper.DistributionType.ALL
        distributionBase = Wrapper.PathBase.GRADLE_USER_HOME
        distributionPath = "wrapper/dists"
        archiveBase = Wrapper.PathBase.GRADLE_USER_HOME
        archivePath = "wrapper/dists"
    }

    patchPluginXml {
        version.set(project.semanticVersion.version.get().toString())
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            projectDir.resolve("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        val latestChangelog = try {
            changelog.getUnreleased()
        } catch (_: MissingVersionException) {
            changelog.getLatest()
        }
        changeNotes.set(provider {
            changelog.renderItem(
                    latestChangelog
                            .withHeader(false)
                            .withEmptySections(false),
                    org.jetbrains.changelog.Changelog.OutputType.HTML
            )
        })
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }


    buildSearchableOptions {
        enabled = false
    }

    jarSearchableOptions {
        enabled = false
    }

    val deleteLog = create("deleteLogs", Delete::class.java) {
        outputs.upToDateWhen { false }
        project.layout.buildDirectory.dir("idea-sandbox/system/log").get().asFile.walk().forEach {
            if (it.name.endsWith(".log")) {
                delete(it)
            }
        }
    }

    runIde {
        dependsOn(deleteLog)
        //rider contributes to prepareSandbox, so it needs to run before runIde
        dependsOn("prepareSandbox")
//        dependsOn(":rider:prepareSandboxForRider")
        maxHeapSize = "2g"
        // Rider's backend doesn't support dynamic plugins. It might be possible to work with auto-reload of the frontend
        // part of a plugin, but there are dangers about keeping plugins in sync
        autoReloadPlugins.set(false)
        systemProperties(
            "idea.log.debug.categories" to "#org.digma",
        )
    }


    //todo: we need to do something with github workflow so that we can verify all required versions,
    // github fails with no space left on device when trying to verify more then 2 IDEs.
    // currently we compile python with IC plus python plugin so no real need to verify pycharm
    // but it would be better if we did.
    listProductsReleases {
        val typesToVerify = properties("typesToVerifyPlugin").split(",")
        types.set(typesToVerify)
        val versionsToVerify = properties("versionsToVerifyPlugin").split(",")
        val lowestVersion = versionsToVerify[0]
        sinceVersion.set(lowestVersion)
        val latestVersion = if(versionsToVerify.size == 1)  versionsToVerify[0] else versionsToVerify[1]
        untilVersion.set(latestVersion)
//        sinceBuild.set("222.3739.36")
//        untilBuild.set("222.4167.24")
        releaseChannels.set(EnumSet.of(ListProductsReleasesTask.Channel.RELEASE))
    }


    runPluginVerifier {
        subsystemsToCheck.set("without-android")
    }

    verifyPlugin {
//        dependsOn(":rider:prepareSandboxForRider")
    }

    signPlugin {
        if (System.getenv("DIGMA_JB_PRIVATE_KEY_PASSWORD") != null) {
            certificateChain.set(System.getenv("DIGMA_JB_CERTIFICATE_CHAIN_FILE").trimIndent())
            privateKey.set(System.getenv("DIGMA_JB_PRIVATE_KEY_FILE").trimIndent())
            password.set(System.getenv("DIGMA_JB_PRIVATE_KEY_PASSWORD").trim())
        }
    }


    publishPlugin {
        if (System.getenv("PUBLISH_TOKEN") != null) {
            token.set(System.getenv("PUBLISH_TOKEN").trim())
        }

        // the version is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(project.semanticVersion.version.get().toString().split('-').getOrElse(1) { "default" }
            .split('.').first()))
    }


    create("printVersion", DefaultTask::class.java) {
        doLast {
            logger.lifecycle("The project current version is ${project.semanticVersion.version.get()}")
        }
    }

}
