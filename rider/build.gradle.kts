import common.IdeFlavor
import common.logBuildProfile
import common.platformVersion
import common.properties
import common.rider.rdLibDirectory
import common.withCurrentProfile
import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress(
    //see: https://youtrack.jetbrains.com/issue/KTIJ-19369
    "DSL_SCOPE_VIOLATION"
)
plugins {
    id("plugin-library")
    id("com.jetbrains.rdgen") version libs.versions.rider.rdgen.get()
}

private val dllOutputFolder = "${projectDir}/Digma.Rider.Plugin/Digma.Rider/bin/Digma.Rider/Debug/"


dependencies {
    implementation(libs.commons.collections4)
    implementation(libs.commons.lang3)
    compileOnly(project(":ide-common"))
    compileOnly(project(":model"))
}


//rider module should always build with RD
val platformType by extra(IdeFlavor.RD.name)

logBuildProfile(project)


intellij {
    version.set("$platformType-${project.platformVersion()}")
    plugins.set(listOf("rider-plugins-appender"))
    downloadSources.set(false) //there are no sources for rider
}



rdgen {

    val modelDir = File(projectDir, "protocol/src/main/kotlin")
    val csOutput = File(projectDir, "Digma.Rider.Plugin/Digma.Rider/Protocol")
    val ktOutput = File(projectDir, "src/main/kotlin/org/digma/intellij/plugin/rider/protocol")

    verbose = true
    classpath(rdLibDirectory(project).canonicalPath + "/rider-model.jar")
    logger.lifecycle("rdLibDirectory is ${rdLibDirectory(project).canonicalPath}")
    sources("${modelDir.canonicalPath}/rider/model")
    hashFolder = project.layout.buildDirectory.asFile.get().canonicalPath
    packages = "rider.model"

    generator {
        language = "kotlin"
        transform = "asis"
        root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
        namespace = "org.digma.rider.protocol"
        directory = ktOutput.canonicalPath
    }

    generator {
        language = "csharp"
        transform = "reversed"
        root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
        namespace = "Digma.Rider.Protocol"
        directory = csOutput.canonicalPath
    }
}


tasks {

    withType<JavaCompile> {
        dependsOn(named("rdgen"))
    }
    withType<KotlinCompile> {
        dependsOn(named("rdgen"))
    }


    val deletePluginProps by registering(Delete::class){
        delete(layout.projectDirectory.dir(properties("DotnetPluginId",project)).file("Plugin.props"))
    }

    val initPluginProps by registering(Copy::class){

        dependsOn(deletePluginProps)

        val tokens = mutableMapOf<String, String>()


        withCurrentProfile {
            tokens["RESHARPER_VERSION"] = it.riderResharperVersion
            tokens["VERSION_CONSTANT"] = it.riderResharperVersionConstant
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(layout.projectDirectory.dir("Digma.Rider.Plugin"))
        include("Plugin.props.template")
        filter<ReplaceTokens>("tokens" to tokens)
        into(layout.projectDirectory.dir("Digma.Rider.Plugin"))
        rename("(.+).template", "$1")
    }


    val setBuildTool by registering {
        doLast {
            val toolArgs = ArrayList<String>()
            extra["executable"] = "dotnet"
            toolArgs.add("msbuild")
            toolArgs.add(properties("DotnetPluginId", project) + "/" + properties("DotnetSolution", project))
            toolArgs.add("/p:Configuration=" + properties("BuildConfiguration", project))
            toolArgs.add("/p:HostFullIdentifier=")

            extra["args"] = toolArgs.toTypedArray()
        }

    }


    val compileDotNet = create("compileDotNet") {

        outputs.file("$dllOutputFolder/Digma.Rider.dll")
        outputs.file("$dllOutputFolder/Digma.Rider.pdb")

        dependsOn(initPluginProps)
        dependsOn(setBuildTool)
        dependsOn(named("rdgen"))

        doLast {
            @Suppress("UNCHECKED_CAST") //we know it's an array, it's built in setBuildTool
            val arguments: MutableList<String> = (setBuildTool.get().extra.get("args") as Array<String>).toMutableList()
            arguments.add("/t:Restore;Rebuild")
            exec {
                executable = setBuildTool.get().extra.get("executable").toString()
                args = arguments
                workingDir = projectDir
            }
        }
    }


    prepareSandbox {
        dependsOn(compileDotNet)
    }

    buildPlugin{
        dependsOn(compileDotNet)
    }


    val cleanRdGen by registering(Delete::class) {
        delete(fileTree("${projectDir}/src/main/kotlin/org/digma/intellij/plugin/rider/protocol/").matching {
            include("*.Generated.kt")
        })
        delete(fileTree("${projectDir}/Digma.Rider.Plugin/Digma.Rider/Protocol/").matching {
            include("*.Generated.cs")
        })
    }

    named("rdgen") {
        dependsOn(cleanRdGen)
    }

    clean{
        dependsOn(cleanRdGen)
        delete("${projectDir}/Digma.Rider.Plugin/Digma.Rider/bin")
        delete("${projectDir}/Digma.Rider.Plugin/Digma.Rider/obj")
        delete("${projectDir}/Digma.Rider.Plugin/Digma.Rider.Tests/bin")
        delete("${projectDir}/Digma.Rider.Plugin/Digma.Rider.Tests/obj")
    }

}


val compileDotNet by tasks
val riderDotNetObjects: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
artifacts {
    add("riderDotNetObjects", file("$dllOutputFolder/Digma.Rider.dll")) {
        builtBy(compileDotNet)
    }
    add("riderDotNetObjects", file("$dllOutputFolder/Digma.Rider.pdb")) {
        builtBy(compileDotNet)
    }
}