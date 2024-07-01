package org.digma.intellij.plugin.startup

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log

abstract class DigmaProjectActivity : ProjectActivity {

    protected val logger = Logger.getInstance(this::class.java)

    abstract fun executeProjectStartup(project: Project)

    override suspend fun execute(project: Project) {
        runCatching {
            Log.log(logger::trace, project, "Project activity {} started", this::class.java.simpleName)
            executeProjectStartup(project)
            Log.log(logger::trace, project, "Project activity {} ended", this::class.java.simpleName)
        }.onFailure {
            ErrorReporter.getInstance().reportError(project, "DigmaProjectActivity.execute", it)
        }
    }
}