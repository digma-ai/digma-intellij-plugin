package org.digma.intellij.plugin.engagement

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import java.io.File

class EngagementScoreServiceStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        try {
            fixFile()
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("EngagementScoreServiceStartup.fixFile", e)
        }

        EngagementScoreService.getInstance()
    }


    //todo:
    // we had a bug that empty lines where added to the file and loading fails.
    // this will fix it.
    // it was added at 24/09/2024, it can be removed after 2-3 months
    private fun fixFile() {
        val path = System.getProperty("idea.config.path")
        val dir = File(path, "options")
        val file = File(dir, "DigmaEngagementScorePersistence.xml")
        val lines = file.readLines()

        val linesToSave = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            if (line != null) {
                if (!badLine(line)) {
                    linesToSave.add(line)
                }
            }
        }


        val modifiedContent = linesToSave.joinToString(System.lineSeparator())
        file.writeText(modifiedContent)
    }

    private fun badLine(line: String): Boolean {
        return line.contains("<entry />") || line.contains("<entry/>") || line.contains("<entry  />") || line.isBlank()
    }


}