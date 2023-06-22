@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.common.buildPreviewListPanel
import org.digma.intellij.plugin.ui.common.statuspanels.createLoadingInsightsPanel
import org.digma.intellij.plugin.ui.common.statuspanels.createNoErrorsEmptyStatePanel
import org.digma.intellij.plugin.ui.common.statuspanels.createPendingInsightsPanel
import org.digma.intellij.plugin.ui.common.statuspanels.createStartupEmptyStatePanel
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.errors.ErrorsPanelList
import org.digma.intellij.plugin.ui.list.insights.PreviewList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.UIInsightsStatus
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JPanel


private val logger: Logger = Logger.getInstance("org.digma.intellij.plugin.ui.insights.ErrorsTab")


fun errorsPanel(project: Project): DigmaTabPanel {

    //errorsModel and insightsModel are not singletons but are single per open project.
    //they are created by the view service and live as long as the project is alive.
    //so components can bind to them, but not to members of them, the model instance is the same on but the
    //members change , like the various lists. or bind to a function of the mode like getScope.
    val errorsModel = ErrorsViewService.getInstance(project).model
    val insightsModel = InsightsViewService.getInstance(project).model

    val errorsList = ScrollablePanelList(ErrorsPanelList(project, errorsModel.listViewItems))
    val previewList = ScrollablePanelList(PreviewList(project, errorsModel.getMethodNamesWithErrors()))

    val previewPanel = buildPreviewListPanel(previewList)

    val errorsDetailsPanel = errorDetailsPanel(project, errorsModel)

    //a card layout for the errorsList or previewList
    val myCardLayout = CardLayout()
    val myCardPanel = JPanel(myCardLayout)
    myCardPanel.isOpaque = false
    myCardPanel.border = empty()

    myCardPanel.add(errorsList, ErrorsTabCard.ERRORS_LIST.name)
    myCardLayout.addLayoutComponent(errorsList, ErrorsTabCard.ERRORS_LIST.name)

    myCardPanel.add(previewPanel, ErrorsTabCard.PREVIEW_LIST.name)
    myCardLayout.addLayoutComponent(previewPanel, ErrorsTabCard.PREVIEW_LIST.name)

    myCardPanel.add(errorsDetailsPanel, ErrorsTabCard.ERROR_DETAILS.name)
    myCardLayout.addLayoutComponent(errorsDetailsPanel, ErrorsTabCard.ERROR_DETAILS.name)

    myCardLayout.show(myCardPanel, ErrorsTabCard.ERRORS_LIST.name)

    val result = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            if (ErrorsTabCard.ERROR_DETAILS == errorsModel.card) {
                return errorsDetailsPanel.getPreferredFocusableComponent()
            }
            return errorsList
        }

        override fun getPreferredFocusedComponent(): JComponent {
            if (ErrorsTabCard.ERROR_DETAILS == errorsModel.card) {
                return errorsDetailsPanel.getPreferredFocusedComponent()
            }
            return errorsList
        }

        //reset must be called from EDT
        override fun reset() {

            errorsList.getModel().setListData(errorsModel.listViewItems)
            previewList.getModel().setListData(errorsModel.getMethodNamesWithErrors())
            errorsDetailsPanel.reset()
            Log.log(logger::debug, project, "Changing errors tab card to ${errorsModel.card.name}")
            myCardLayout.show(myCardPanel, errorsModel.card.name)
        }
    }

    result.layout = BorderLayout()
    result.add(myCardPanel, BorderLayout.CENTER)
    result.background = listBackground()

    return wrapWithEmptyStatuses(project,result, errorsModel, insightsModel)
}




private fun wrapWithEmptyStatuses(
    project: Project,
    errorsPanel: DigmaTabPanel,
    errorsModel: ErrorsModel,
    insightsModel: InsightsModel
): DigmaTabPanel {

    //using the insights model status to change to empty state card

    val noErrorsCardName = "NoErrors"

    val emptyStatusesCardsLayout = CardLayout()
    val emptyStatusesCardsPanel = JPanel(emptyStatusesCardsLayout)
    emptyStatusesCardsPanel.isOpaque = false
    emptyStatusesCardsPanel.border = empty()

    val noInErrorsPanel = createNoErrorsEmptyStatePanel()
    val pendingInsightsPanel = createPendingInsightsPanel()
    val loadingInsightsPanel = createLoadingInsightsPanel()

    val startupEmpty = createStartupEmptyStatePanel(project)

    emptyStatusesCardsPanel.add(errorsPanel, UIInsightsStatus.Default.name)
    emptyStatusesCardsLayout.addLayoutComponent(errorsPanel, UIInsightsStatus.Default.name)

    emptyStatusesCardsPanel.add(noInErrorsPanel, noErrorsCardName)
    emptyStatusesCardsLayout.addLayoutComponent(noInErrorsPanel, noErrorsCardName)

    emptyStatusesCardsPanel.add(loadingInsightsPanel, UIInsightsStatus.Loading.name)
    emptyStatusesCardsLayout.addLayoutComponent(loadingInsightsPanel, UIInsightsStatus.Loading.name)

    emptyStatusesCardsPanel.add(pendingInsightsPanel, UIInsightsStatus.InsightPending.name)
    emptyStatusesCardsLayout.addLayoutComponent(pendingInsightsPanel, UIInsightsStatus.InsightPending.name)

    emptyStatusesCardsPanel.add(startupEmpty, UIInsightsStatus.Startup.name)
    emptyStatusesCardsLayout.addLayoutComponent(startupEmpty, UIInsightsStatus.Startup.name)

    emptyStatusesCardsLayout.show(emptyStatusesCardsPanel,UIInsightsStatus.Startup.name)



    val resultPanel = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return errorsPanel
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return errorsPanel
        }

        override fun reset() {

            errorsPanel.reset()

            if (insightsModel.status == UIInsightsStatus.Startup){
                emptyStatusesCardsLayout.show(emptyStatusesCardsPanel,UIInsightsStatus.Startup.name)
            }else if (errorsModel.card == ErrorsTabCard.ERROR_DETAILS){
                Log.log(logger::debug, project, "Changing to error default")
                emptyStatusesCardsLayout.show(emptyStatusesCardsPanel,UIInsightsStatus.Default.name )
            }else if (listOf(UIInsightsStatus.Loading,UIInsightsStatus.InsightPending).contains(insightsModel.status)){
                Log.log(logger::debug, project, "Changing to card  ${insightsModel.status.name}")
                emptyStatusesCardsLayout.show(emptyStatusesCardsPanel, insightsModel.status.name)
            }else{
                var cardToShow = UIInsightsStatus.Default.name

                if (errorsModel.listViewItems.isNotEmpty() &&
                    listOf(UIInsightsStatus.NoSpanData,UIInsightsStatus.NoObservability,UIInsightsStatus.NoInsights).contains(insightsModel.status)){
                    cardToShow = noErrorsCardName
                }

                if (errorsModel.listViewItems.isEmpty() && errorsModel.card == ErrorsTabCard.ERRORS_LIST){
                    cardToShow = noErrorsCardName
                }
                if (!errorsModel.hasErrors() && errorsModel.card == ErrorsTabCard.PREVIEW_LIST){
                    cardToShow = noErrorsCardName
                }

                Log.log(logger::debug, project, "Changing to card  $cardToShow")
                emptyStatusesCardsLayout.show(emptyStatusesCardsPanel, cardToShow)
            }
        }
    }

    resultPanel.layout = BorderLayout()
    resultPanel.add(emptyStatusesCardsPanel,BorderLayout.CENTER)
    resultPanel.border = empty()
    resultPanel.background = listBackground()

    return resultPanel
}
