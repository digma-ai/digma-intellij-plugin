package org.digma.intellij.plugin.notifications

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class InsightsNotificationStartupActivity: StartupActivity {
    override fun runActivity(project: Project) {
        project.service<InsightsNotificationsService>().waitForFirstImportantInsight()
    }
}