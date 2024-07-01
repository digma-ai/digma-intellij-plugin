package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class RecentActivitiesStartup : DigmaProjectActivity() {
    override fun executeProjectStartup(project: Project) {
        //only initialize it, it will start an update thread
        project.service<RecentActivityUpdater>()
    }
}