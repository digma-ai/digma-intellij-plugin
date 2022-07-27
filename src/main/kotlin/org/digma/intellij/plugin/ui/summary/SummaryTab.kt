package org.digma.intellij.plugin.ui.summary

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.ui.common.EnvironmentsPanel
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.list.summaries.SummaryPanelList
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.SummaryViewService
import java.awt.BorderLayout
import javax.swing.JComponent

fun summaryPanel(project: Project): DigmaTabPanel {

    val model = SummaryViewService.getInstance(project).model

    val analyticsService: AnalyticsService = AnalyticsService.getInstance(project)
    val envsPanel = EnvironmentsPanel(project, model, analyticsService.environment)
    envsPanel.border = empty(10)

    val summaryList = ScrollablePanelList(SummaryPanelList(project, model.insights))

    val result = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return envsPanel
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return summaryList
        }

        //reset must be called from EDT
        override fun reset() {
            envsPanel.reset()
            summaryList.getModel().setListData(model.insights)
        }
    }

    result.layout = BorderLayout()
    result.add(envsPanel, BorderLayout.NORTH)
    result.add(summaryList, BorderLayout.CENTER)
    result.background = listBackground()
    return result
}