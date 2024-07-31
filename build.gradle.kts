import common.buildVersion
import common.currentProfile
import common.dynamicPlatformType
import common.platformVersion
import common.properties
import common.withCurrentProfile
import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.changelog.date
import org.jetbrains.changelog.exceptions.MissingVersionException
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

fun properties(key: String) = properties(key, project)

plugins {
    id("semantic-version")
    id("plugin-project")
    id("org.jetbrains.changelog") version "2.2.0"
    id("org.jetbrains.qodana") version "0.1.13"
    id("org.jetbrains.kotlinx.kover") version "0.7.5"

}


//the platformType determines which platform we build with, it may be IC,IU,PC,PY,RD.
//it is a good way for us to test that we don't have leaking usages of classes that exist in one platform
//but don't exist in another.
//this module should be compatible with all IDEs, and thus it should build with IC,IU,RD with no issues.
//the platformType is determined dynamically based on other property,
// we have for example buildWithRider,buildWIthUltimate.
//the other modules either also build with dynamic platform type or always build with the same type.
val platformType: IntelliJPlatformType by extra(dynamicPlatformType(project))


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

    //modules that are not plugin modules should be added as implementation.
    //plugin modules, modules with org.jetbrains.intellij.platform.module, should be added as
    // pluginModule in the intellijPlatform dependencies extension.

    implementation(libs.commons.lang3)
    implementation(libs.freemarker)
    implementation(project(":model"))
    implementation(project(":analytics-provider"))

    riderDotNetObjects(
        project(
            mapOf(
                "path" to ":rider",
                "configuration" to "riderDotNetObjects"
            )
        )
    )

    intellijPlatform {

        //this module is the main plugin module and should not depend on any intellij bundled plugin, the code here should
        // be common to all IDEs. specific modules depend on specific plugins, for example the jvm-common
        // module depends on java and kotlin, ide-common depends on git, python depends on python plugin etc.

        val version = project.platformVersion()
        //the intellij product. we use the create method because it may be Idea,Rider,Pycharm etc.
        create(platformType, version)

        pluginModule(implementation(project(":ide-common")))
        pluginModule(implementation(project(":jvm-common")))
        pluginModule(implementation(project(":gradle-support")))
        pluginModule(implementation(project(":maven-support")))
        pluginModule(implementation(project(":rider")))

        pluginVerifier()
        zipSigner()
    }
}

configurations.getByName("runtimeClasspath") {
    //make sure we never package kotlin-stdlib-jdk8 or kotlin-stdlib-jdk7 because it is supplied by the IDE.
    //see more in
    //https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
    //settings.gradle.kts
    exclude("org.jetbrains.kotlin", "kotlin-stdlib")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
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



intellijPlatform {

    pluginConfiguration {

        id = "org.digma.intellij"
        name = properties("pluginName")
        version = project.buildVersion()
        description = layout.projectDirectory.file("PLUGIN-DESCRIPTION.md").asFile.readText()

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


        ideaVersion {
            sinceBuild = project.currentProfile().pluginSinceBuild
            untilBuild = project.currentProfile().pluginUntilBuild
        }

        vendor {
            name = "Digma.ai"
            email = "info@digma.ai"
            url = "https://digma.ai/"
        }
    }


    //todo: add a publish workflow for alpha,beta etc.
    //if the build version may contain the channel like 2.0.342+241-alpha,
    // we can do that instead of just default:
    // channels = listOf(project.buildVersion().split('-').getOrElse(1) { "default" }.split('.').first())
    // but we don't support that yet
    if (System.getenv("PUBLISH_TOKEN") != null) {
        publishing {
            token = System.getenv("PUBLISH_TOKEN").trim()
            channels = listOf("default")
            ideServices = false
            hidden = false
        }
    }

    if (System.getenv("DIGMA_JB_PRIVATE_KEY_PASSWORD") != null) {
        signing {
            certificateChain = System.getenv("DIGMA_JB_CERTIFICATE_CHAIN_FILE").trimIndent()
            privateKey = System.getenv("DIGMA_JB_PRIVATE_KEY_FILE").trimIndent()
            password = System.getenv("DIGMA_JB_PRIVATE_KEY_PASSWORD").trim()
        }
    }


    //todo: run plugin verifier for resharper: https://blog.jetbrains.com/dotnet/2023/05/26/the-api-verifier/
    verifyPlugin {
        //our plugin id is "org.digma.intellij", lately jetbrains added a check that
        // plugin id doesn't contain the word intellij and treats it as error. so we need
        // '-mute TemplateWordInPluginId' to silence this error.
        freeArgs = listOf("-mute", "TemplateWordInPluginId")

        ides {
            //use the same platformType and version as in intellijPlatform dependencies
            //Note: recommended() doesn't work well, sometimes tries to resolve a wrong IDE and fails in GitHub
            withCurrentProfile { buildProfile ->
                ide(platformType,buildProfile.platformVersion)

                if (!buildProfile.isEAP) {
                    select {
                        types =
                            listOf(IntelliJPlatformType.IntellijIdeaCommunity, IntelliJPlatformType.IntellijIdeaUltimate)
                        channels = listOf(ProductRelease.Channel.RELEASE)
                        sinceBuild = project.currentProfile().pluginSinceBuild
                        untilBuild = project.currentProfile().pluginUntilBuild
                    }
                }
            }
        }
        subsystemsToCheck = VerifyPluginTask.Subsystems.WITHOUT_ANDROID
    }
}



tasks {

    prepareSandbox {

        //copy rider dlls to the plugin sandbox, so it is packaged in the zip
        from(configurations.getByName("riderDotNetObjects")) {
            into("${rootProject.name}/dotnet/")
        }

        //check that we have 2 files
        doLast {
            val files = configurations.getByName("riderDotNetObjects").files
            if (files.size != 2) {
                throw RuntimeException("wrong number of files in riderDotNetObjects.")
            }
        }
    }


    wrapper {
        //to upgrade gradle change the version here and run:
        //./gradlew wrapper --gradle-version 8.8
        //check that gradle/wrapper/gradle-wrapper.properties was changed
        gradleVersion = "8.8"
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


    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
//    runIdeForUiTests {
//        systemProperty("robot-server.port", "8082")
//        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
//        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
//        systemProperty("jb.consents.confirmation.enabled", "false")
//    }


    val deleteLog by registering(Delete::class) {
        outputs.upToDateWhen { false }
        val ideFolderName = if(platformType == IntelliJPlatformType.Rider){
            "${platformType.code}-${project.currentProfile().riderVersion}"
        }else{
            "${platformType.code}-${project.currentProfile().platformVersion}"
        }
        project.layout.buildDirectory.dir("idea-sandbox/$ideFolderName/log").get().asFile.walk().forEach {
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

        maxHeapSize = "6g"

        systemProperties(
            "idea.log.trace.categories" to "#org.digma",
            "idea.log.debug.categories" to "#org.digma",
            //make large idea.log because the default rotates every 10M and makes it difficult to follow messages with tail
            "idea.log.limit" to "999999999",
            "idea.trace.stub.index.update" to "true",
            "org.digma.plugin.enable.devtools" to "true",
            "kotlinx.coroutines.debug" to "",
            "org.digma.plugin.report.all.errors" to "true",

//            "idea.ProcessCanceledException" to "disabled"


            //to use a local file for digma-agent or digma extension,
            // usually when developing and we want to test the plugin
            //see org.digma.intellij.plugin.idea.execution.OtelAgentPathProvider
            //don't forget to comment when done testing !
            //"digma.agent.override.path" to "/home/shalom/workspace/digma/digma-agent/build/libs/digma-agent-1.0.10-SNAPSHOT.jar"
            //"digma.otel.extension.override.path" to "/home/shalom/workspace/digma/otel-java-instrumentation/agent-extension/build/libs/digma-otel-agent-extension-0.8.12.jar"
            //can also change the url from where the jar is downloaded when IDE starts
            //"org.digma.otel.extensionUrl" to "some url
            //"org.digma.otel.digmaAgentUrl" to "some url

        )
    }


    val posthogTokenUrlFile = file("${project.sourceSets.main.get().output.resourcesDir?.absolutePath}/posthog-token-url.txt")
    val injectPosthogTokenUrlTask by registering {

        inputs.property("token",System.getenv("POSTHOG_TOKEN_URL") ?: "")
        outputs.files(posthogTokenUrlFile)

        doLast {
            logger.lifecycle("injecting posthog token url")
            val url = System.getenv("POSTHOG_TOKEN_URL") ?: ""
            posthogTokenUrlFile.writeText(url)
        }
    }


    jar{
        inputs.files(injectPosthogTokenUrlTask)
    }


    processResources {
        finalizedBy(injectPosthogTokenUrlTask)

        exclude("**/webview/global-env-vars.txt")

        val filesToFilter = listOf(
            "webview/recentactivity/recentActivityTemplate.ftl",
            "webview/navigation/navigationtemplate.ftl",
            "webview/jaegerui/jaegeruitemplate.ftl",
            "webview/documentation/documentation.ftl",
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
