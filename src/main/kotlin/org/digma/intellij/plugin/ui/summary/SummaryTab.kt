package org.digma.intellij.plugin.ui.summary

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.wrapWithNoConnectionWrapper
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.list.summaries.SummaryPanelList
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.SummaryViewService
import java.awt.BorderLayout
import javax.swing.JComponent

fun summaryPanel(project: Project): DigmaTabPanel {

    val model = SummaryViewService.getInstance(project).model

    val summaryList = ScrollablePanelList(SummaryPanelList(project, model.insights))

    val result = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return summaryList
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return summaryList
        }

        //reset must be called from EDT
        override fun reset() {
            summaryList.getModel().setListData(model.insights)
        }
    }

    result.isOpaque = false
    result.border = JBUI.Borders.empty()
    result.layout = BorderLayout()
    result.add(summaryList, BorderLayout.CENTER)
    result.background = listBackground()

    return wrapWithNoConnectionWrapper(project, result)
}