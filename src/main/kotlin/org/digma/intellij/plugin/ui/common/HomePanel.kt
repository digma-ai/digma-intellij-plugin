package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.SummaryViewService
import org.digma.intellij.plugin.ui.service.TabsHelper
import org.digma.intellij.plugin.ui.summary.summaryPanel
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

class HomePanel(project: Project) : DigmaResettablePanel() {


    private val tabbedPane = JBTabbedPane()


    init {

        isOpaque = false
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty()

        add(tabbedPane)
        tabbedPane.isOpaque = false
        tabbedPane.border = JBUI.Borders.empty()

        val assetsPanel = createAssetsPanel(project);
        tabbedPane.addTab(TabsHelper.ASSETS_TAB_NAME, assetsPanel)

        val summaryPanel = createSummaryPanel(project);
        tabbedPane.addTab(TabsHelper.DASHBOARD_TAB_NAME, summaryPanel)



    }



    override fun reset() {

    }


    private fun createAssetsPanel(project: Project): Component? {
        val assetsPanel = JPanel()
        assetsPanel.add(JLabel("Assets: TBD"))
        return assetsPanel
    }



    private fun createSummaryPanel(project: Project): DigmaTabPanel {
        val summaryPanel = summaryPanel(project)
        val summaryViewService = project.getService(SummaryViewService::class.java)
        summaryViewService.panel = summaryPanel
        return summaryPanel
    }
}