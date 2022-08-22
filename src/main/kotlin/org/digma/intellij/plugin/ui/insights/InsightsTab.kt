@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.insights

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.common.createTopPanel
import org.digma.intellij.plugin.ui.common.noCodeObjectWarningPanel
import org.digma.intellij.plugin.ui.common.wrapWithNoConnectionWrapper
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.insights.InsightsList
import org.digma.intellij.plugin.ui.list.insights.PreviewList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


private const val NO_INFO_CARD_NAME = "NO-INFO"

fun insightsPanel(project: Project): DigmaTabPanel {

    //errorsModel and insightsModel are not singletons but are single per open project.
    //they are created by the view service and live as long as the project is alive.
    //so components can bind to them, but not to members of them, the model instance is the same on but the
    //members change , like the various lists. or bind to a function of the mode like getScope.
    val insightsModel = InsightsViewService.getInstance(project).model

    val topPanelWrapper = createTopPanel(project, insightsModel)


    val insightsList = ScrollablePanelList(InsightsList(project, insightsModel.listViewItems))
    val previewList = ScrollablePanelList(PreviewList(project, insightsModel.previewListViewItems))

    val previewTitle = panel {
        row {
            icon(AllIcons.Ide.FatalErrorRead)
                .horizontalAlign(HorizontalAlign.CENTER)
        }
        row {
            label("No code object was selected")
                .horizontalAlign(HorizontalAlign.CENTER)
        }
        row {
            label("").bind(
                JLabel::getText, JLabel::setText, MutableProperty(
                    getter = { insightsModel.getPreviewListMessage() },
                    setter = {})
            )
        }
    }

    previewTitle.isOpaque = false

    val previewPanel = JPanel(BorderLayout())
    previewPanel.add(previewTitle, BorderLayout.NORTH)
    previewPanel.add(previewList, BorderLayout.CENTER)
    previewPanel.isOpaque = false


    val noInfoWarningPanel = noCodeObjectWarningPanel(insightsModel)

    val cardLayout = CardLayout()
    val cardsPanel = JPanel(cardLayout)
    cardsPanel.isOpaque = false
    cardsPanel.add(insightsList, InsightsTabCard.INSIGHTS.name)
    cardsPanel.add(previewPanel, InsightsTabCard.PREVIEW.name)
    cardsPanel.add(noInfoWarningPanel, NO_INFO_CARD_NAME)
    cardLayout.addLayoutComponent(insightsList, InsightsTabCard.INSIGHTS.name)
    cardLayout.addLayoutComponent(previewPanel, InsightsTabCard.PREVIEW.name)
    cardLayout.addLayoutComponent(noInfoWarningPanel, NO_INFO_CARD_NAME)
    cardLayout.show(cardsPanel, NO_INFO_CARD_NAME)

    val result = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return topPanelWrapper
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return topPanelWrapper
        }

        //reset must be called from EDT
        override fun reset() {

            noInfoWarningPanel.reset()
            topPanelWrapper.reset()
            previewTitle.reset()

            insightsList.getModel().setListData(insightsModel.listViewItems)
            previewList.getModel().setListData(insightsModel.previewListViewItems)

            if (insightsList.getModel().size == 0 && insightsModel.card.equals(InsightsTabCard.INSIGHTS)) {
                cardLayout.show(cardsPanel, NO_INFO_CARD_NAME)
            } else if (previewList.getModel().size == 0 && insightsModel.card.equals(InsightsTabCard.PREVIEW)) {
                cardLayout.show(cardsPanel, NO_INFO_CARD_NAME)
            } else {
                cardLayout.show(cardsPanel, insightsModel.card.name)
            }

            revalidate()
            repaint()
        }
    }

    result.layout = BorderLayout()
    result.add(topPanelWrapper, BorderLayout.NORTH)
    result.add(cardsPanel, BorderLayout.CENTER)
    result.background = listBackground()

    return wrapWithNoConnectionWrapper(project, result)
}






