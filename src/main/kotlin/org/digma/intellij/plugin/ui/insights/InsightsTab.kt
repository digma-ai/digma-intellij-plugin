@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.insights

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.common.buildPreviewListPanel
import org.digma.intellij.plugin.ui.common.statuspanels.createLoadingInsightsPanel
import org.digma.intellij.plugin.ui.common.statuspanels.createNoDataYetEmptyStatePanel
import org.digma.intellij.plugin.ui.common.statuspanels.createNoInsightsPanel
import org.digma.intellij.plugin.ui.common.statuspanels.createNoObservabilityPanel
import org.digma.intellij.plugin.ui.common.statuspanels.createPendingInsightsPanel
import org.digma.intellij.plugin.ui.common.statuspanels.createStartupEmptyStatePanel
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.insights.InsightsList
import org.digma.intellij.plugin.ui.list.insights.PreviewList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.UIInsightsStatus
import org.digma.intellij.plugin.ui.model.insights.InsightGroupListViewItem
import org.digma.intellij.plugin.ui.model.insights.InsightListViewItem
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JPanel


private val logger: Logger = Logger.getInstance("org.digma.intellij.plugin.ui.insights.InsightsTab")

fun insightsPanel(project: Project): DigmaTabPanel {

    //errorsModel and insightsModel are not singletons but are single per open project.
    //they are created by the view service and live as long as the project is alive.
    //so components can bind to them, but not to members of them, the model instance is the same always but the
    //members change , like the various lists. or bind to a function of the model like getScope.
    val insightsModel = InsightsViewService.getInstance(project).model

    val insightsList = ScrollablePanelList(InsightsList(project, insightsModel.listViewItems))
    val previewList = ScrollablePanelList(PreviewList(project,insightsModel.getMethodNamesWithInsights()))

    val previewPanel = buildPreviewListPanel(previewList)

    val myCardLayout = CardLayout()
    val myCardsPanel = JPanel(myCardLayout)
    myCardsPanel.isOpaque = false
    myCardsPanel.border = empty()
    myCardsPanel.add(insightsList, InsightsTabCard.INSIGHTS.name)
    myCardsPanel.add(previewPanel, InsightsTabCard.PREVIEW.name)
    myCardLayout.addLayoutComponent(insightsList, InsightsTabCard.INSIGHTS.name)
    myCardLayout.addLayoutComponent(previewPanel, InsightsTabCard.PREVIEW.name)
    myCardLayout.show(myCardsPanel, InsightsTabCard.INSIGHTS.name)

    val result = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return insightsList
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return insightsList
        }

        //reset must be called from EDT
        override fun reset() {

            insightsList.getModel().setListData(insightsModel.listViewItems)
            previewList.getModel().setListData(insightsModel.getMethodNamesWithInsights())
            Log.log(logger::debug, project, "Changing insights tab card to ${insightsModel.card.name}")
            myCardLayout.show(myCardsPanel, insightsModel.card.name)

            val insightTypes = insightsModel.listViewItems
                .filterIsInstance<InsightGroupListViewItem>()
                .flatMap { it.modelObject }
                .filterIsInstance<InsightListViewItem<*>>()
                .map { it.insightType }
                .distinct()
            if(insightTypes.isNotEmpty()){
                ActivityMonitor.getInstance(project).registerInsightsViewed(insightTypes)
            }
        }
    }

    result.layout = BorderLayout()
    result.add(myCardsPanel, BorderLayout.CENTER)
    result.background = listBackground()

    return wrapWithEmptyStatuses(project, result,insightsModel)
}






private fun wrapWithEmptyStatuses(project: Project, insightsPanel: DigmaTabPanel, insightsModel: InsightsModel): DigmaTabPanel {

    val emptyStatusesCardsLayout = CardLayout()
    val emptyStatusesCardsPanel = JPanel(emptyStatusesCardsLayout)
    emptyStatusesCardsPanel.isOpaque = false
    emptyStatusesCardsPanel.border = empty()

    val noInsightsPanel = createNoInsightsPanel()
    val pendingInsightsPanel = createPendingInsightsPanel()
    val loadingInsightsPanel = createLoadingInsightsPanel()
    val noDataYetPanel = createNoDataYetEmptyStatePanel()
    val noObservabilityPanel = createNoObservabilityPanel(project, insightsModel)

    val startupEmpty = createStartupEmptyStatePanel(project)

    emptyStatusesCardsPanel.add(insightsPanel, UIInsightsStatus.Default.name)
    emptyStatusesCardsLayout.addLayoutComponent(insightsPanel, UIInsightsStatus.Default.name)

    emptyStatusesCardsPanel.add(noInsightsPanel, UIInsightsStatus.NoInsights.name)
    emptyStatusesCardsLayout.addLayoutComponent(noInsightsPanel, UIInsightsStatus.NoInsights.name)

    emptyStatusesCardsPanel.add(loadingInsightsPanel, UIInsightsStatus.Loading.name)
    emptyStatusesCardsLayout.addLayoutComponent(loadingInsightsPanel, UIInsightsStatus.Loading.name)

    emptyStatusesCardsPanel.add(pendingInsightsPanel, UIInsightsStatus.InsightPending.name)
    emptyStatusesCardsLayout.addLayoutComponent(pendingInsightsPanel, UIInsightsStatus.InsightPending.name)

    emptyStatusesCardsPanel.add(noDataYetPanel, UIInsightsStatus.NoSpanData.name)
    emptyStatusesCardsLayout.addLayoutComponent(noDataYetPanel, UIInsightsStatus.NoSpanData.name)

    emptyStatusesCardsPanel.add(noObservabilityPanel, UIInsightsStatus.NoObservability.name)
    emptyStatusesCardsLayout.addLayoutComponent(noObservabilityPanel, UIInsightsStatus.NoObservability.name)

    emptyStatusesCardsPanel.add(startupEmpty, UIInsightsStatus.Startup.name)
    emptyStatusesCardsLayout.addLayoutComponent(startupEmpty, UIInsightsStatus.Startup.name)

    emptyStatusesCardsLayout.show(emptyStatusesCardsPanel,UIInsightsStatus.Startup.name)


    val resultPanel = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return insightsPanel
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return insightsPanel
        }

        override fun reset() {

            insightsPanel.reset()

            if (insightsModel.status == UIInsightsStatus.NoObservability){
                noObservabilityPanel.reset()
            }

            if (insightsModel.status == UIInsightsStatus.Startup){
                emptyStatusesCardsLayout.show(emptyStatusesCardsPanel,UIInsightsStatus.Startup.name)
            }else{
                Log.log(logger::debug, project, "Changing to empty state card  ${insightsModel.status.name}")
                emptyStatusesCardsLayout.show(emptyStatusesCardsPanel, insightsModel.status.name)
            }

        }
    }

    resultPanel.layout = BorderLayout()
    resultPanel.add(emptyStatusesCardsPanel,BorderLayout.CENTER)
    resultPanel.border = empty()
    resultPanel.background = listBackground()

    return resultPanel
}
