import common.buildVersion
import common.dynamicPlatformType
import common.logBuildProfile
import common.platformPlugins
import common.platformVersion
import common.properties
import common.shouldDownloadSources
import common.withCurrentProfile
import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.changelog.date
import org.jetbrains.changelog.exceptions.MissingVersionException
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.tasks.ListProductsReleasesTask
import java.util.EnumSet

fun properties(key: String) = properties(key,project)

@Suppress(
    //see: https://youtrack.jetbrains.com/issue/KTIJ-19369
    "DSL_SCOPE_VIOLATION"
)
plugins {
    id("semantic-version")
    id("plugin-project")
    id("org.jetbrains.changelog") version "2.2.0"
    id("org.jetbrains.qodana") version "0.1.13"
    id("org.jetbrains.kotlinx.kover") version "0.7.5"

}

//the platformType is determined dynamically with a gradle property.
//it enables launching different IDEs with different versions and still let the other modules
//compile correctly. most modules always compile with the same platform type.
//it is only necessary for launcher, so when launching rider the platform type for this project and ide-common
// should be RD but not for the other projects like java,python.
val platformType: String by extra(dynamicPlatformType(project))

logBuildProfile(project)

tasks.register("printCurrentProfileBuildVersion") {
    doLast {
        println("build-version=${project.buildVersion()}")
    }
}


//this project depends on rider dotnet artifacts. this will force the dotnet build before packaging.
val riderDotNetObjects: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {

    //todo: enable instrumentedJar : https://github.com/digma-ai/digma-intellij-plugin/issues/1729

    implementation(libs.commons.lang3)
    implementation(libs.freemarker)
    implementation(project(":model"))
    implementation(project(":analytics-provider"))
    implementation(project(":ide-common"))
    implementation(project(":jvm-common"))
    implementation(project(":python"))
    implementation(project(":rider"))

    riderDotNetObjects(project(mapOf(
        "path" to ":rider",
        "configuration" to "riderDotNetObjects")))
}

configurations {
    runtimeClasspath {
        //make sure we never package kotlin-stdlib-jdk8 or kotlin-stdlib-jdk7 because its is supplied by the IDE.
        //see more in
        //buildSrc/src/main/kotlin/digma-base.gradle.kts
        //settings.gradle.kts
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
    }
}


intellij {

    val platformType = properties("platformType")
    val platformPlugins = project.platformPlugins().split(',').map(String::trim).filter(String::isNotEmpty)
    val platformVersion = project.platformVersion()

    println("Running with PlatformType: $platformType")
    println("Running with PlatformPlugins: $platformPlugins")
    println("Running with PlatformVersion: $platformVersion")

    pluginName.set(properties("pluginName"))
    version.set(platformVersion)
    type.set(platformType)
    plugins.set(platformPlugins)
    downloadSources.set(project.shouldDownloadSources()) //todo: probably not necessary because the default is to check CI env

    pluginsRepositories {
        marketplace()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    //change log needs the base version,not the build version
    val versionToUse = common.semanticversion.getSemanticVersion(project)
    version.set(versionToUse)
    path.set("${project.projectDir}/CHANGELOG.md")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
    header.set(provider { "[${versionToUse}] - ${date()}" })
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
    //but this syntax is not favorite by the gradle developers because it will cause eager initialization of the task.
    val buildPlugin = tasks.named("buildPlugin").get()
    project(":jvm-common").afterEvaluate { buildPlugin.dependsOn(tasks.getByName("buildPlugin")) }
    project(":python").afterEvaluate { buildPlugin.dependsOn(tasks.getByName("buildPlugin")) }
    project(":rider").afterEvaluate { buildPlugin.dependsOn(tasks.getByName("buildPlugin")) }
}



tasks {

    prepareSandbox{
        //copy rider dlls to the plugin sandbox so it is packaged in the zip
        from(configurations.getByName("riderDotNetObjects")){
            into("${properties("pluginName",project)}/dotnet/")
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
        distributionType = Wrapper.DistributionType.ALL
        distributionBase = Wrapper.PathBase.GRADLE_USER_HOME
        distributionPath = "wrapper/dists"
        archiveBase = Wrapper.PathBase.GRADLE_USER_HOME
        archivePath = "wrapper/dists"
    }

    patchChangelog {
        outputs.upToDateWhen { false }
        doLast {
            logger.lifecycle("in patchChangelog, releaseNote=$releaseNote, version=${version.get()}, inputFile=${inputFile.get()}, outputFile=${outputFile.get()}")
        }
    }

    patchPluginXml {
        version.set(project.buildVersion())
        withCurrentProfile {
            sinceBuild.set(it.pluginSinceBuild)
            untilBuild.set(it.pluginUntilBuild)
        }

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

    val deleteLog by registering(Delete::class) {
        outputs.upToDateWhen { false }
        project.layout.buildDirectory.dir("idea-sandbox/system/log").get().asFile.walk().forEach {
            if (it.name.endsWith(".log")) {
                delete(it)
            }
        }
    }


    runIde {
        dependsOn(deleteLog)

        //to disable the splash screen on startup because it may interrupt when debugging.
        args(listOf("nosplash"))

//        jvmArgs("-XX:ReservedCodeCacheSize=512M")

        maxHeapSize = "2g"
        // Rider's backend doesn't support dynamic plugins. It might be possible to work with auto-reload of the frontend
        // part of a plugin, but there are dangers about keeping plugins in sync
        autoReloadPlugins.set(false)
        systemProperties(
            "idea.log.trace.categories" to "#org.digma",
            "idea.log.debug.categories" to "#org.digma",
            //make large idea.log because the default rotates every 10M and makes it difficult to follow messages with tail
            "idea.log.limit" to "999999999",
            "idea.trace.stub.index.update" to "true",
            "org.digma.plugin.enable.devtools" to "true",

//            "idea.ProcessCanceledException" to "disabled"
        )


        //todo: this is a workaround for these issues:
        // 241 should run with JBR 21. but currently there is a bug in gradle-intellij-plugin that downloads
        // a 21 JBR without jcef support.
        // see this issue:
        // https://github.com/JetBrains/gradle-intellij-plugin/issues/1534
        // and this PR fixes it
        // https://github.com/JetBrains/gradle-intellij-plugin/pull/1535
        // when gradle-intellij-plugin has a new release including the above fix it will download the right 21 JBR.
        // but then there is this issue:
        // https://github.com/digma-ai/digma-intellij-plugin/issues/1734
        // if 241 runs with 21 then gradle-intellij-plugin will configure the test task to use this JBR,
        // but some of our tests fail with 21. so we have to make sure all our tests pass with JBR 21
        // before removing this workaround.
        //Note in build 241.11761.10 jetbrains changed the bundled JBR to 17
//        if (project.currentProfile().profile == BuildProfiles.Profiles.p241) {
//            jbrVersion = "17.0.10b1171.14"
//        }
    }


    listProductsReleases {
        types.set(listOf(platformType))
        //doesn't work for EAP , but runPluginVerifier does not rely on the output of listProductsReleases
        withCurrentProfile { profile ->
            sinceBuild.set(profile.pluginSinceBuild)
            untilBuild.set(profile.pluginUntilBuild)
        }

        releaseChannels.set(EnumSet.of(ListProductsReleasesTask.Channel.RELEASE))
    }


    //todo: run plugin verifier for resharper
    // https://blog.jetbrains.com/dotnet/2023/05/26/the-api-verifier/
    runPluginVerifier {

        //rider EAP doesn't work here, plugin verifier can't find it
        withCurrentProfile { profile ->
            if (profile.isEAP) {
                enabled = false
            } else {
                ideVersions.set(listOf("${platformType}-${profile.versionToRunPluginVerifier}"))
            }
        }
        subsystemsToCheck.set("without-android")
    }

    verifyPlugin {
        dependsOn(prepareSandbox)
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
        channels.set(listOf("default"))
//        channels.set(listOf(project.buildVersion().split('-').getOrElse(1) { "default" }
//            .split('.').first()))
    }


    val injectPosthogTokenUrlTask by registering{
        doLast{
            logger.lifecycle("injecting posthog token url")
            val url = System.getenv("POSTHOG_TOKEN_URL") ?: ""
            file("${project.sourceSets.main.get().output.resourcesDir?.absolutePath}/posthog-token-url.txt").writeText(url)
        }
    }
    processResources{
        finalizedBy(injectPosthogTokenUrlTask)

        exclude("**/webview/global-env-vars.txt")

        val filesToFilter = listOf(
            "webview/recentactivity/recentActivityTemplate.ftl",
            "webview/tests/testsTemplate.ftl",
            "webview/notifications/notificationstemplate.ftl",
            "webview/assets/assetstemplate.ftl",
            "webview/navigation/navigationtemplate.ftl",
            "webview/insights/insightstemplate.ftl",
            "webview/jaegerui/jaegeruitemplate.ftl",
            "webview/main/maintemplate.ftl"
        )


        val globalEnvVarsFilePath = "src/main/resources/webview/global-env-vars.txt"
        val globalEnvVarsFile = layout.projectDirectory.file(globalEnvVarsFilePath).asFile

        inputs.file(globalEnvVarsFile.canonicalPath)
        //all files are inputs so a change will trigger this task to filter again
        filesToFilter.forEach {
            inputs.file(layout.projectDirectory.file("src/main/resources/$it").asFile.canonicalPath)
        }

        val globalEnvVars = globalEnvVarsFile.readText()
        val tokens = mutableMapOf<String, String>()
        tokens["GLOBAL_ENV_VARS"] = globalEnvVars

        filesMatching(filesToFilter) {
            filter<ReplaceTokens>("tokens" to tokens)
        }
    }
}