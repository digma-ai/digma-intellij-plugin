package org.digma.intellij.plugin.ui.summary

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.createSummaryEmptyPanel
import org.digma.intellij.plugin.ui.common.wrapWithNoConnectionWrapper
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.list.summaries.SummaryPanelList
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.SummaryViewService
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JPanel

private const val EMPTY_CARD_NAME = "EmptySummary"
private const val LIST_CARD_NAME = "FullSummary"

fun summaryPanel(project: Project): DigmaTabPanel {

    val model = SummaryViewService.getInstance(project).model

    val summaryList = ScrollablePanelList(SummaryPanelList(project, model.insights))
    val emptyStatePanel = createSummaryEmptyPanel()

    val cardLayout = CardLayout()
    val cardsPanel = JPanel(cardLayout)
    cardsPanel.isOpaque = false
    cardsPanel.border = JBUI.Borders.empty()
    cardsPanel.add(summaryList, LIST_CARD_NAME)
    cardsPanel.add(emptyStatePanel, EMPTY_CARD_NAME)
    cardLayout.show(cardsPanel, EMPTY_CARD_NAME)

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
            if (model.insights.isEmpty()){
                cardLayout.show(cardsPanel, EMPTY_CARD_NAME)
            }else{
                cardLayout.show(cardsPanel, LIST_CARD_NAME)
            }
        }
    }

    result.isOpaque = false
    result.border = JBUI.Borders.empty()
    result.layout = BorderLayout()
    result.add(cardsPanel, BorderLayout.CENTER)
    result.background = listBackground()

    return wrapWithNoConnectionWrapper(project, result)
}