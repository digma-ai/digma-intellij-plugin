import com.jetbrains.plugin.structure.base.utils.isFile
import common.BuildProfiles
import common.BuildProfiles.greaterThan
import common.currentProfile
import common.withCurrentProfile
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
private val buildConfiguration = "Debug"
private val solutionFile = "$dotnetPluginId/Digma.Rider.Plugin.sln"
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
}




//rider module should always build with RD
val platformType: IntelliJPlatformType by extra(IntelliJPlatformType.Rider)


dependencies {
    implementation(libs.commons.collections4)
    implementation(libs.commons.lang3)
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))

    intellijPlatform {
        rider(project.currentProfile().riderVersion)
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
    logger.lifecycle("Rider Sdk Path: $riderSdkPath")
    logger.lifecycle("Rider Sdk project file: $riderSdkProjectFile")
    logger.lifecycle("Rider Sdk test project file: $riderSdkTestProjectFile")
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

            logger.lifecycle("Writing nuget.config to $path, content $content")
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
        val traceSourceVersion = if(project.currentProfile().profile.greaterThan(BuildProfiles.Profiles.p233)){
            "4.3.0"
        }else{
            "4.0.0"
        }

        withCurrentProfile {
            tokens["DOTNET_SDK_PATH"] = "$riderSdkPath".replace(Regex("/*$"), "")
            tokens["DOTNET_SDK_TEST_PROJECT"] = Path.of(riderSdkTestProjectFilePath).toString()
            tokens["TRACE_SOURCE_VERSION"] = traceSourceVersion
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
        dependsOn(generateNuGetConfig, initPluginProps,initPluginTestProps, rdGen)
    }


    val compileDotNet by registering {

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
            //dotnet msbuild rider/build/dotnet/msbuild.binlog  /flp:v=diag
            //it will create msbuild.log file in the current directory.
            //the command will also create msbuild.log in the rider directory, this is the /fl.
            //for diagnostics add argument "/v:diag"

            //about /r: in development we sometimes need to build with different profiles, for example 232 and then 241.
            // sometimes the conpileDotnet task will fail and looks like its using the wrong assemblies. /r(estore) fixes it.

            logger.lifecycle("compileDotNet:Plugin.props: ${pluginPropsFile.readText()}")
            logger.lifecycle("compileDotNet:nuget.config: ${nugetConfigFile.readText()}")

            exec {
                executable = "dotnet"
                args = listOf(
                    "msbuild",
                    "/r",
                    "/p:Configuration=$buildConfiguration",
                    "/t:Clean;Restore;Rebuild",
                    "/nodeReuse:False",
                    "/fl",
                    "/bl:${project.layout.buildDirectory.get().asFile.absolutePath}/dotnet/msbuild.binlog",
                    solutionFile
                )
                workingDir = projectDir
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
            logger.lifecycle("rider-model.jar: $it")
            check(it.isFile) {
                "rider-model.jar is not found at $it"
            }
        }
    }) {
        builtBy(Constants.Tasks.INITIALIZE_INTELLIJ_PLATFORM_PLUGIN)
    }
}

