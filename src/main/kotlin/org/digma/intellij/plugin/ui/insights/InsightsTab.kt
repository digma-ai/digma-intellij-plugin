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
import org.digma.intellij.plugin.ui.common.*
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



private val logger: Logger = Logger.getInstance("org.digma.intellij.plugin.ui.insights.InsightsTab")

fun insightsPanel(project: Project): DigmaTabPanel {

    //errorsModel and insightsModel are not singletons but are single per open project.
    //they are created by the view service and live as long as the project is alive.
    //so components can bind to them, but not to members of them, the model instance is the same on but the
    //members change , like the various lists. or bind to a function of the mode like getScope.
    val insightsModel = InsightsViewService.getInstance(project).model

    val insightsList = ScrollablePanelList(InsightsList(project, insightsModel.listViewItems))

    val previewList = ScrollablePanelList(PreviewList(project,insightsModel.getMethodNamesWithInsights()))

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

    val noInsightsPanel = createNoInsightsPanel()
    val pendingInsightsPanel = createPendingInsightsPanel()
    val loadingInsightsPanel = createLoadingInsightsPanel()
    val noDataYetPanel = createNoDataYetPanel()
    val noObservabilityPanel = createNoObservabilityPanel(project, insightsModel)

    val cardLayout = CardLayout()
    val cardsPanel = JPanel(cardLayout)
    cardsPanel.isOpaque = false
    cardsPanel.border = JBUI.Borders.empty()
    cardsPanel.add(insightsList, InsightsTabCard.INSIGHTS.name)
    cardsPanel.add(previewPanel, InsightsTabCard.PREVIEW.name)
    cardsPanel.add(noInsightsPanel, UiInsightStatus.NoInsights.name)
    cardsPanel.add(loadingInsightsPanel, UiInsightStatus.LoadingInsights.name)
    cardsPanel.add(pendingInsightsPanel, UiInsightStatus.InsightPending.name)
    cardsPanel.add(noDataYetPanel, UiInsightStatus.NoSpanData.name)
    cardsPanel.add(noObservabilityPanel, UiInsightStatus.NoObservability.name)
    cardLayout.addLayoutComponent(insightsList, InsightsTabCard.INSIGHTS.name)
    cardLayout.addLayoutComponent(previewPanel, InsightsTabCard.PREVIEW.name)
    cardLayout.addLayoutComponent(noInsightsPanel, UiInsightStatus.NoInsights.name)
    cardLayout.addLayoutComponent(loadingInsightsPanel, UiInsightStatus.LoadingInsights.name)
    cardLayout.addLayoutComponent(pendingInsightsPanel, UiInsightStatus.InsightPending.name)
    cardLayout.addLayoutComponent(noDataYetPanel, UiInsightStatus.NoSpanData.name)
    cardLayout.addLayoutComponent(noObservabilityPanel, UiInsightStatus.NoObservability.name)
    cardLayout.show(cardsPanel, UiInsightStatus.NoInsights.name)

    val result = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return insightsList
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return insightsList
        }

        //reset must be called from EDT
        override fun reset() {

            previewTitle.reset()

            insightsList.getModel().setListData(insightsModel.listViewItems)
            previewList.getModel().setListData(insightsModel.getMethodNamesWithInsights())
            if (insightsList.getModel().size == 0 && insightsModel.card == InsightsTabCard.INSIGHTS) {
                val cardName = when (insightsModel.status) {
                    UiInsightStatus.InsightPending -> UiInsightStatus.InsightPending.name
                    UiInsightStatus.NoSpanData -> UiInsightStatus.NoSpanData.name
                    UiInsightStatus.NoObservability ->{
                        noObservabilityPanel.reset()
                        UiInsightStatus.NoObservability.name
                    }
                    // because of sync issues between the model (updating in the background) and the current backend call.
                    // model has no insights and the current backend call returns insights exists, the model will be updated in x seconds (with insights) so its ok to show no insights for this period of time
                    UiInsightStatus.InsightExist -> UiInsightStatus.InsightPending.name
                    UiInsightStatus.NoInsights -> UiInsightStatus.NoInsights.name
                    UiInsightStatus.Unknown -> UiInsightStatus.LoadingInsights.name
                    else -> UiInsightStatus.NoInsights.name
                }
                cardLayout.show(cardsPanel, cardName)
            } else if (!insightsModel.hasInsights()
                    && insightsModel.card == InsightsTabCard.PREVIEW) {
                if(insightsModel.hasDiscoverableCodeObjects()){
                    cardLayout.show(cardsPanel, UiInsightStatus.NoSpanData.name)  //show no data yet
                }
                else{
                    cardLayout.show(cardsPanel, UiInsightStatus.NoInsights.name)
                }

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