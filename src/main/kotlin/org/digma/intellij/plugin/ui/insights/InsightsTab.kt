@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.insights

import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.common.createLoadingInsightsPanel
import org.digma.intellij.plugin.ui.common.createNoDataYetPanel
import org.digma.intellij.plugin.ui.common.createNoObservabilityPanel
import org.digma.intellij.plugin.ui.common.createPendingInsightsPanel
import org.digma.intellij.plugin.ui.common.noCodeObjectWarningPanel
import org.digma.intellij.plugin.ui.common.wrapWithNoConnectionWrapper
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.insights.InsightsList
import org.digma.intellij.plugin.ui.list.insights.PreviewList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.insights.InsightListViewItem
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.digma.intellij.plugin.ui.model.insights.UiInsightStatus
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


private const val NO_INFO_CARD_NAME = "NO-INFO"
private const val LOADING_INSIGHTS_CARD_NAME = "Loading-Insights"


private val logger: Logger = Logger.getInstance("org.digma.intellij.plugin.ui.insights.InsightsTab")

fun insightsPanel(project: Project): DigmaTabPanel {

    //errorsModel and insightsModel are not singletons but are single per open project.
    //they are created by the view service and live as long as the project is alive.
    //so components can bind to them, but not to members of them, the model instance is the same on but the
    //members change , like the various lists. or bind to a function of the mode like getScope.
    val insightsModel = InsightsViewService.getInstance(project).model

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
    val pendingInsightsPanel = createPendingInsightsPanel()
    val loadingInsightsPanel = createLoadingInsightsPanel()
    val noDataYetPanel = createNoDataYetPanel()
    val noObservabilityPanel = createNoObservabilityPanel()

    val cardLayout = CardLayout()
    val cardsPanel = JPanel(cardLayout)
    cardsPanel.isOpaque = false
    cardsPanel.border = JBUI.Borders.empty()
    cardsPanel.add(insightsList, InsightsTabCard.INSIGHTS.name)
    cardsPanel.add(previewPanel, InsightsTabCard.PREVIEW.name)
    cardsPanel.add(noInfoWarningPanel, NO_INFO_CARD_NAME)
    cardsPanel.add(loadingInsightsPanel, LOADING_INSIGHTS_CARD_NAME)
    cardsPanel.add(pendingInsightsPanel, UiInsightStatus.InsightPending.name)
    cardsPanel.add(noDataYetPanel, UiInsightStatus.NoSpanData.name)
    cardsPanel.add(noObservabilityPanel, UiInsightStatus.NoObservability.name)
    cardLayout.addLayoutComponent(insightsList, InsightsTabCard.INSIGHTS.name)
    cardLayout.addLayoutComponent(previewPanel, InsightsTabCard.PREVIEW.name)
    cardLayout.addLayoutComponent(noInfoWarningPanel, NO_INFO_CARD_NAME)
    cardLayout.show(cardsPanel, NO_INFO_CARD_NAME)

    val result = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return insightsList
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return insightsList
        }

        //reset must be called from EDT
        override fun reset() {

            noInfoWarningPanel.reset()
            previewTitle.reset()

            insightsList.getModel().setListData(insightsModel.listViewItems)
            previewList.getModel().setListData(insightsModel.previewListViewItems)

            if (insightsList.getModel().size == 0 && insightsModel.card == InsightsTabCard.INSIGHTS) {
                val cardName = when (insightsModel.status) {
                    UiInsightStatus.Unknown -> LOADING_INSIGHTS_CARD_NAME
                    UiInsightStatus.InsightPending -> UiInsightStatus.InsightPending.name
                    UiInsightStatus.NoSpanData -> UiInsightStatus.NoSpanData.name
                    UiInsightStatus.NoObservability -> UiInsightStatus.NoObservability.name
                    else -> NO_INFO_CARD_NAME
                }
                cardLayout.show(cardsPanel, cardName)
            } else if (previewList.getModel().size == 0 && insightsModel.card == InsightsTabCard.PREVIEW) {
                cardLayout.show(cardsPanel, NO_INFO_CARD_NAME)
            } else {
                Log.log(logger::debug, project, "Changing insights tab card to ${insightsModel.card.name}")
                cardLayout.show(cardsPanel, insightsModel.card.name)
            }

            revalidate()
            repaint()

            val insightTypes = insightsModel.listViewItems
                .filter { it is InsightGroupListViewItem }
                .flatMap { (it as InsightGroupListViewItem).modelObject }
                .filter { it is InsightListViewItem<*> }
                .map { (it as InsightListViewItem<*>).insightType }
                .distinct()
            if(!insightTypes.isEmpty()){
                ActivityMonitor.getInstance(project).registerInsightsViewed(insightTypes)
            }
        }
    }

    result.layout = BorderLayout()
    result.add(cardsPanel, BorderLayout.CENTER)
    result.background = listBackground()
    result.isOpaque = false

    return wrapWithNoConnectionWrapper(project, result)
}