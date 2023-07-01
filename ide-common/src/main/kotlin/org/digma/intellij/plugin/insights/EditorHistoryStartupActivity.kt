package org.digma.intellij.plugin.insights

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class EditorHistoryStartupActivity : StartupActivity, DumbAware {
    override fun runActivity(project: Project) {

        project.service<InsightsViewOrchestrator>()
            .startupFiles(EditorHistoryManager.getInstance(project).fileList.map { virtualFile -> virtualFile.url }.toSet())
    }
}