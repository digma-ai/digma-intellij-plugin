package org.digma.intellij.plugin.engagement

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class EngagementScoreServiceStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        EngagementScoreService.getInstance()
    }
}