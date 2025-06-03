package common

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.util.Properties

open class DownloadOtelJarsTask : DefaultTask() {

    @InputFile
    val propsFile = project.objects.fileProperty()

    @OutputDirectory
    val outputDir = project.objects.directoryProperty()

    @TaskAction
    fun downloadJars() {
        val props = Properties().apply {
            propsFile.get().asFile.inputStream().use { load(it) }
        }

        val nameMap = mapOf(
            "otel-agent" to "opentelemetry-javaagent.jar",
            "digma-extension" to "digma-otel-agent-extension.jar",
            "digma-agent" to "digma-agent.jar"
        )

        val dir = outputDir.get().asFile
        dir.mkdirs()

        props.forEach { (key, value) ->
            val finalName = nameMap[key] ?: throw GradleException("Unknown key in properties file: $key")
            val url = value.toString()
            val outputFile = dir.resolve(finalName)

            logger.lifecycle("Downloading $url → ${outputFile.name}")
            withRetry(maxAttempts = 3, delayMillis = 5_000) {
                URI(url).toURL().openStream().use { input ->
                    input.copyTo(outputFile.outputStream())
                }
            }
        }
    }

}