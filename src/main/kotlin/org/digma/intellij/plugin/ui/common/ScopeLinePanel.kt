package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.model.ModelChangeListener
import org.digma.intellij.plugin.refreshInsightsTask.RefreshService
import org.digma.intellij.plugin.ui.errors.GeneralRefreshIconButton
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JPanel


class ScopeLinePanel(project: Project) : JPanel(BorderLayout()) {


    init {

        isOpaque = false
        border = JBUI.Borders.empty()

        val insightsModel = project.service<InsightsViewService>().model
        val scopeLine =
            scopeLine(project, { insightsModel.getScopeString() }, { insightsModel.getScopeTooltip() }, ScopeLineIconProducer(insightsModel))

        val buttonsPanel = JPanel(GridLayout(1, 2, 5, 0))
        buttonsPanel.isOpaque = false
        buttonsPanel.border = JBUI.Borders.empty()
        buttonsPanel.add(getCodeLocationButton(project))
        buttonsPanel.add(getGeneralRefreshButton(project))
        this.add(scopeLine, BorderLayout.CENTER)
        this.add(buttonsPanel, BorderLayout.EAST)

        project.messageBus.connect(project.service<InsightsViewService>()).subscribe(
            ModelChangeListener.MODEL_CHANGED_TOPIC, ModelChangeListener
            { scopeLine.reset() }
        )
    }


    private fun getCodeLocationButton(project: Project): JButton {
        val size = Laf.scalePanels(Laf.Sizes.BUTTON_SIZE_24)
        val buttonsSize = Dimension(size, size)
        val button = CodeNavigationButton(project)
        button.preferredSize = buttonsSize
        button.maximumSize = buttonsSize
        return button
    }


    private fun getGeneralRefreshButton(project: Project): JButton {
        val size = Laf.scalePanels(Laf.Sizes.BUTTON_SIZE_24)
        val buttonsSize = Dimension(size, size)
        val generalRefreshIconButton = GeneralRefreshIconButton(project, Laf.Icons.Insight.REFRESH)
        generalRefreshIconButton.preferredSize = buttonsSize
        generalRefreshIconButton.maximumSize = buttonsSize
        generalRefreshIconButton.toolTipText = asHtml("Refresh")
        generalRefreshIconButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        generalRefreshIconButton.addActionListener {
            project.service<RefreshService>().refreshAllInBackground()
        }
        return generalRefreshIconButton
    }
}