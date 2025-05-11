package common

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI

open class DownloadUiBundle : DefaultTask() {

    @InputFile
    val uiVersionFile = project.objects.fileProperty()

    @OutputDirectory
    val outputDir = project.objects.directoryProperty()

    @TaskAction
    fun download() {
        val uiVersion = uiVersionFile.get().asFile.readText().trim()
        val url = "https://github.com/digma-ai/digma-ui/releases/download/v$uiVersion/dist-jetbrains-v$uiVersion.zip"
        val fileName = "digma-ui-$uiVersion.zip"
        val targetFile = outputDir.get().file(fileName).asFile

        logger.lifecycle("Downloading $url → ${targetFile.name}")

        withRetry(maxAttempts = 3, delayMillis = 5000) {
            URI(url).toURL().openStream().use { input ->
                input.copyTo(targetFile.outputStream())
            }
        }
    }

}