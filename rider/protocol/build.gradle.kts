plugins {
    id("common-kotlin")
    id("rdgen-version")
    id("com.jetbrains.rdgen") version libs.versions.rider.rdgen.get()
}

group = "org.digma.plugins.rider.protocol"
version = "0.0.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.rdGen)
    implementation(
        project(
            mapOf(
                "path" to ":rider",
                "configuration" to "riderModel"
            )
        )
    )
}


val csOutput = File(project(":rider").projectDir, "Digma.Rider.Plugin/Digma.Rider/Protocol")
val ktOutput = File(project(":rider").projectDir, "src/main/kotlin/org/digma/intellij/plugin/rider/protocol")



rdgen {

    val modelDir = File(projectDir, "src/main/kotlin")
    verbose = true
    classpath(sourceSets["main"].compileClasspath)
    sources("${modelDir.canonicalPath}/rider/model")
    hashFolder = project(":rider").layout.buildDirectory.asFile.get().canonicalPath
    packages = "rider.model"

    generator {
        language = "kotlin"
        transform = "asis"
        root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
        namespace = "org.digma.rider.protocol"
        directory = ktOutput.canonicalPath
        generatedFileSuffix = ".Generated"
    }

    generator {
        language = "csharp"
        transform = "reversed"
        root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
        namespace = "Digma.Rider.Protocol"
        directory = csOutput.canonicalPath
        generatedFileSuffix = ".Generated"
    }
}

tasks {

    build {
        dependsOn("rdgen")
    }

    val cleanRdGen by registering(Delete::class) {
        delete(fileTree(ktOutput).matching {
            include("*.Generated.kt")
        })
        delete(fileTree(csOutput).matching {
            include("*.Generated.cs")
        })
    }

    clean {
        dependsOn(cleanRdGen)
    }
}