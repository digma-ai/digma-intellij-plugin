package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.log.Log

class RecentActivityToolWindowFactory : ToolWindowFactory {

    private val logger = Logger.getInstance(this::class.java)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

        Log.log(logger::info, project, "creating recent activity tool window for project  {}", project)

        //initialize AnalyticsService early so the UI can detect the connection status when created
        project.service<AnalyticsService>()

        project.service<RecentActivityService>()

        project.service<RecentActivityToolWindowShower>().toolWindow = toolWindow

        val recentActivityPanel = RecentActivityPanel(project)

        val content = ContentFactory.getInstance().createContent(recentActivityPanel, null, false)

        toolWindow.contentManager.addContent(content)

    }
}