package org.digma.intellij.plugin.engagement

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EngagementScoreServiceStartup : ProjectActivity {

    private val fixFileLock = ReentrantLock(true)

    val logger = Logger.getInstance(this::class.java)

    override suspend fun execute(project: Project) {
        try {
            if (!PersistenceService.getInstance().isEngagementScorePersistenceFileFixed()) {
                fixFileLock.withLock {
                    if (!PersistenceService.getInstance().isEngagementScorePersistenceFileFixed()) {
                        fixFile()
                        PersistenceService.getInstance().setEngagementScorePersistenceFileFixed()
                    }
                }
            }
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error fixing file")
            ErrorReporter.getInstance().reportError("EngagementScoreServiceStartup.fixFile", e)
        }

        EngagementScoreService.getInstance()
    }


    //todo:
    // we had a bug that empty lines where added to the file and loading fails.
    // this will fix it.
    // it was added at 24/09/2024, it can be removed after 2-3 months
    private fun fixFile() {

        Log.log(logger::trace, "trying to fix file DigmaEngagementScorePersistence.xml")

        val file = findFile()

        Log.log(logger::trace, "found file {}", file)

        val lines = file.readLines()

        val linesToSave = mutableListOf<String>()

        var badLinesFound = false

        lines.forEachIndexed { index, line ->
            if (line != null) {
                Log.log(logger::trace, "found line {}", line)
                if (badLine(line)) {
                    badLinesFound = true
                } else {
                    linesToSave.add(line)
                }
            }
        }

        if (!badLinesFound) {
            Log.log(logger::trace, "file {} doesn't have bad lines", file)
            return
        }

        Log.log(logger::trace, "fixing file {} with new lines", file)
        if (logger.isTraceEnabled) {
            linesToSave.forEach {
                Log.log(logger::trace, "new line {}", it)
            }
        }

        val modifiedContent = linesToSave.joinToString(System.lineSeparator())

        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "saving new content to file {}", modifiedContent)
        }

        file.writeText(modifiedContent)
    }

    private fun findFile(): File {
        val path = System.getProperty("idea.config.path") ?: PathManager.getConfigPath()
        val dir = File(path, "options")
        return File(dir, "DigmaEngagementScorePersistence.xml")
    }

//    private fun searchConfigPath(): String? {
//        //search known directories
//        return null
//    }

    private fun badLine(line: String): Boolean {
        return line.contains("<entry />") || line.contains("<entry/>") || line.contains("<entry  />") || line.isBlank()
    }


}