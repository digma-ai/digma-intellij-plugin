import com.jetbrains.plugin.structure.base.utils.isFile
import common.DIGMA_NO_INFO_LOGGING
import common.currentProfile
import common.withCurrentProfile
import common.withSilenceLogging
import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory


plugins {
    id("plugin-library")
}

private val dotnetPluginId = "Digma.Rider.Plugin"
private val buildConfiguration = "Debug" //todo: change to release
private val solutionFile = "Digma.Rider.Plugin.sln"
private val dotnetProjectDir = layout.projectDirectory.dir(dotnetPluginId)
private val nugetConfigFile = dotnetProjectDir.file("nuget.config").asFile
private val pluginPropsFile = dotnetProjectDir.file("Plugin.props").asFile
private val pluginTestPropsFile = dotnetProjectDir.file("Plugin.Test.props").asFile
private val dllOutputFolder = dotnetProjectDir.dir("Digma.Rider/bin/Digma.Rider/$buildConfiguration")
private val digmaDll = "$dllOutputFolder/Digma.Rider.dll"
private val digmaPbd = "$dllOutputFolder/Digma.Rider.pdb"
private val rdGen = ":rider:protocol:rdgen"
private val riderSdkProjectFilePath = "/Build/PackageReference.JetBrains.Rider.RdBackend.Common.Props"
private val riderSdkTestProjectFilePath = "/Build/PackageReference.JetBrains.Rider.SDK.Tests.Props"

fun deleteOutputs() {
    delete(dotnetProjectDir.dir("Digma.Rider/bin"))
    delete(dotnetProjectDir.dir("Digma.Rider/obj"))
    delete(dotnetProjectDir.dir("Digma.Rider.Tests/bin"))
    delete(dotnetProjectDir.dir("Digma.Rider.Tests/obj"))
    delete(dotnetProjectDir.file("msbuild.log"))
}


//rider module should always build with RD
val platformType: IntelliJPlatformType by extra(IntelliJPlatformType.Rider)


dependencies {
    implementation(libs.commons.collections4)
    implementation(libs.commons.lang3)
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))

    intellijPlatform {
        //can't use binary installer for rider because we need the maven artifact that contains resharper sdk.
        //when building with rider, two artifacts will be downloaded, a maven artifact for this project, and a binary release
        // for the other projects like ide-common. ide-common doesn't need the maven artifact, so it can build with binary artifact.
        //when running Rider with runIde, it will use a binary installer because the main root project uses an installer.
        //only this module needs the maven artifact for the resharper SDK.
        rider(project.currentProfile().riderVersion, false)
//        bundledPlugin("rider-plugins-appender")
    }
}

val riderSdkPath by lazy {
    val path = intellijPlatform.platformPath.resolve("lib/DotNetSdkForRdPlugins").absolute()
    if (!path.isDirectory()) error("$path does not exist or not a directory")
    return@lazy path
}

val riderSdkProjectFile by lazy {
    return@lazy file("$riderSdkPath".replace(Regex("/*$"), "") + riderSdkProjectFilePath)
}

val riderSdkTestProjectFile by lazy {
    return@lazy file("$riderSdkPath".replace(Regex("/*$"), "") + riderSdkTestProjectFilePath)
}

project.afterEvaluate {
    withSilenceLogging {
        logger.lifecycle("Rider Sdk Path: $riderSdkPath")
        logger.lifecycle("Rider Sdk project file: $riderSdkProjectFile")
        logger.lifecycle("Rider Sdk test project file: $riderSdkTestProjectFile")
    }
}


tasks {

    withType<JavaCompile> {
        dependsOn(rdGen)
    }
    withType<KotlinCompile> {
        dependsOn(rdGen)
    }

    val deleteNuGetConfig by registering(Delete::class) {
        delete(nugetConfigFile)
    }

    val generateNuGetConfig by registering {
        notCompatibleWithConfigurationCache("generateNuGetConfig is not yet compatible with configuration cache")

        inputs.property("profile", project.currentProfile().profile)
        inputs.dir(riderSdkPath)
        inputs.file(riderSdkProjectFile)

        outputs.file(nugetConfigFile)

        doLast {
            val content =
                """
            <?xml version="1.0" encoding="utf-8"?>
            <!-- Auto-generated from 'generateNuGetConfig's -->
            <!-- Run `gradlew :prepare` to regenerate -->
            <configuration>
                <packageSources>
                    <add key="rider-sdk" value="$riderSdkPath" />
                </packageSources>
            </configuration>
            """.trimIndent()

            withSilenceLogging {
                logger.lifecycle("Writing nuget.config to $path, content $content")
            }
            val bytes = content.toByteArray()
            nugetConfigFile.writeBytes(bytes)
        }
    }


    val deletePluginProps by registering(Delete::class) {
        delete(pluginPropsFile)
        delete(pluginTestPropsFile)
    }

    val initPluginProps by registering(Copy::class) {

        inputs.property("profile", project.currentProfile().profile)
        inputs.dir(riderSdkPath)
        inputs.file(riderSdkProjectFile)

        outputs.file(pluginPropsFile)


        val tokens = mutableMapOf<String, String>()

        withCurrentProfile {
            tokens["DOTNET_SDK_PATH"] = "$riderSdkPath".replace(Regex("/*$"), "")
            tokens["DOTNET_SDK_PROJECT"] = Path.of(riderSdkProjectFilePath).toString()
            tokens["TARGET_FRAMEWORK"] = it.riderTargetFramework
            tokens["VERSION_CONSTANT"] = it.riderResharperVersionConstant
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(dotnetProjectDir)
        include("Plugin.props.template")
        filter<ReplaceTokens>("tokens" to tokens)
        into(dotnetProjectDir)
        rename("(.+).template", "$1")
    }


    val initPluginTestProps by registering(Copy::class) {

        mustRunAfter(initPluginProps)

        inputs.property("profile", project.currentProfile().profile)
        inputs.dir(riderSdkPath)
        inputs.file(riderSdkProjectFile)

        outputs.file(pluginTestPropsFile)

        val tokens = mutableMapOf<String, String>()

        //up to p233 the version should be 4.0.0, and 4.3.0 after p233
//        val traceSourceVersion = if (project.currentProfile().profile.greaterThan(BuildProfiles.Profile.p233)) {
//            "4.3.0"
//        } else {
//            "4.0.0"
//        }

        withCurrentProfile {
            tokens["DOTNET_SDK_PATH"] = "$riderSdkPath".replace(Regex("/*$"), "")
            tokens["DOTNET_SDK_TEST_PROJECT"] = Path.of(riderSdkTestProjectFilePath).toString()
//            tokens["TRACE_SOURCE_VERSION"] = traceSourceVersion
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(dotnetProjectDir)
        include("Plugin.Test.props.template")
        filter<ReplaceTokens>("tokens" to tokens)
        into(dotnetProjectDir)
        rename("(.+).template", "$1")
    }


    //call prepare before loading the project to Rider
    val prepare by registering {
        dependsOn(generateNuGetConfig, initPluginProps, initPluginTestProps, rdGen)
    }


    val compileDotNet by registering {
        notCompatibleWithConfigurationCache("compileDotNet is not yet compatible with configuration cache")

        dependsOn(rdGen)

        inputs.property("profile", project.currentProfile().profile)
        inputs.dir(riderSdkPath)
        inputs.file(riderSdkProjectFile)
        inputs.file(riderSdkTestProjectFile)
        inputs.files(generateNuGetConfig)
        inputs.files(initPluginProps)
        inputs.files(initPluginTestProps)

        outputs.file(digmaDll)
        outputs.file(digmaPbd)

        doLast {

            //msbuild command reference: https://learn.microsoft.com/en-us/visualstudio/msbuild/msbuild-command-line-reference?view=vs-2022

            //the dotnet command will generate a binlog.
            //to view the binlog run:
            //dotnet msbuild rider/build/dotnet/msbuild.binlog /flp:v=diag,
            //it will create the msbuild.log file in the current directory.
            //the command will also create msbuild.log in the rider directory, this is the /fl.
            //for diagnostics add the argument "/v:diag"

            //about /r: in development we sometimes need to build with different profiles, for example, 232 and then 241.
            // sometimes the compileDotnet task will fail and looks like it's using the wrong assemblies. /r(estore) fixes it.

            withSilenceLogging {
                logger.lifecycle("compileDotNet:Plugin.props: ${pluginPropsFile.readText()}")
                logger.lifecycle("compileDotNet:nuget.config: ${nugetConfigFile.readText()}")
            }

            val argsList = mutableListOf(
                "msbuild",
                "/r",
                "/p:Configuration=$buildConfiguration",
                "/t:Rebuild",
                "/nodeReuse:False",
                "/fl",
                "/bl:${project.layout.buildDirectory.get().asFile.absolutePath}/dotnet/msbuild.binlog"
            )

            if (project.hasProperty(DIGMA_NO_INFO_LOGGING)) {
                argsList.add("-noConsoleLogger")
            }

            argsList.add(solutionFile)

            withSilenceLogging {
                logger.lifecycle("dotnet args: [${argsList.joinToString(" ")}]")
            }

            exec {
                executable = "dotnet"
                args = argsList.toList()

                workingDir = dotnetProjectDir.asFile
            }
        }
    }


    //buildPlugin depends on build, declared in common-build-logic/src/main/kotlin/plugin-library.gradle.kts
    build {
        dependsOn(compileDotNet)
    }


    val deleteOutputs by registering(Delete::class) {
        deleteOutputs()
    }


    clean {
        dependsOn(deleteNuGetConfig)
        dependsOn(deletePluginProps)
        dependsOn(deleteOutputs)
        delete(layout.projectDirectory.file("msbuild.log"))
    }

}


val compileDotNet by tasks
val riderDotNetObjects: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
artifacts {
    add("riderDotNetObjects", file(digmaDll)) {
        builtBy(compileDotNet)
    }
    add("riderDotNetObjects", file(digmaPbd)) {
        builtBy(compileDotNet)
    }
}


val riderModel: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
artifacts {
    add(riderModel.name, provider {
        intellijPlatform.platformPath.resolve("lib/rd/rider-model.jar").also {
            withSilenceLogging {
                logger.lifecycle("rider-model.jar: $it")
            }

            check(it.isFile) {
                "rider-model.jar is not found at $it"
            }
        }
    }) {
        builtBy(Constants.Tasks.INITIALIZE_INTELLIJ_PLATFORM_PLUGIN)
    }
}

