package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.awt.BorderLayout

fun createMainSidePaneWindowPanel(project: Project): DigmaResettablePanel {

    val insightsModel = InsightsViewService.getInstance(project).model
    val navigationPanel = NavigationPanel(project, insightsModel, AnalyticsService.getInstance(project).environment)
    val tabsPanel = TabsPanel(project)

    val result = object : DigmaResettablePanel() {

        override fun reset() {
            navigationPanel.reset()
            tabsPanel.reset()
        }

        override fun requestFocus() {
            navigationPanel.requestFocus()
        }

        override fun requestFocusInWindow(): Boolean {
            return navigationPanel.requestFocusInWindow()
        }
    }

    result.isOpaque = false
    result.border = JBUI.Borders.empty()
    result.layout = BorderLayout()

    result.add(navigationPanel, BorderLayout.NORTH)
    result.add(tabsPanel, BorderLayout.CENTER)
    return result
}