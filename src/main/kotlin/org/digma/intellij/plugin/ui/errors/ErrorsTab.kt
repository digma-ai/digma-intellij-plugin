@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.common.statuspanels.createNoErrorsEmptyStatePanel
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.errors.ErrorsPanelList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JPanel


private val logger: Logger = Logger.getInstance("org.digma.intellij.plugin.ui.insights.ErrorsTab")


fun errorsPanel(project: Project): DigmaTabPanel {

    val errorsModel = ErrorsViewService.getInstance(project).model

    val errorsList = ScrollablePanelList(ErrorsPanelList(project, errorsModel.listViewItems))

    val errorsDetailsPanel = errorDetailsPanel(project, errorsModel)

    //a card layout for the errorsList or previewList
    val myCardLayout = CardLayout()
    val myCardPanel = JPanel(myCardLayout)
    myCardPanel.isOpaque = false
    myCardPanel.border = empty()

    myCardPanel.add(errorsList, ErrorsTabCard.ERRORS_LIST.name)
    myCardLayout.addLayoutComponent(errorsList, ErrorsTabCard.ERRORS_LIST.name)

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
            errorsDetailsPanel.reset()
            Log.log(logger::debug, project, "Changing errors tab card to ${errorsModel.card.name}")
            myCardLayout.show(myCardPanel, errorsModel.card.name)
        }
    }

    result.layout = BorderLayout()
    result.add(myCardPanel, BorderLayout.CENTER)
    result.background = listBackground()

    return wrapWithEmptyStatuses(project, result, errorsModel)
}




private fun wrapWithEmptyStatuses(
    project: Project,
    errorsPanel: DigmaTabPanel,
    errorsModel: ErrorsModel,
): DigmaTabPanel {

    val defaultCardName = "Default"
    val noErrorsCardName = "NoErrors"

    val emptyStatusesCardsLayout = CardLayout()
    val emptyStatusesCardsPanel = JPanel(emptyStatusesCardsLayout)
    emptyStatusesCardsPanel.isOpaque = false
    emptyStatusesCardsPanel.border = empty()

    val noInErrorsPanel = createNoErrorsEmptyStatePanel()

    emptyStatusesCardsPanel.add(errorsPanel, defaultCardName)
    emptyStatusesCardsLayout.addLayoutComponent(errorsPanel, defaultCardName)

    emptyStatusesCardsPanel.add(noInErrorsPanel, noErrorsCardName)
    emptyStatusesCardsLayout.addLayoutComponent(noInErrorsPanel, noErrorsCardName)

    emptyStatusesCardsLayout.show(emptyStatusesCardsPanel, defaultCardName)

    val resultPanel = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return errorsPanel
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return errorsPanel
        }

        override fun reset() {

            errorsPanel.reset()

            var cardToShow = defaultCardName

            if (errorsModel.listViewItems.isEmpty()) {
                cardToShow = noErrorsCardName
            }


            Log.log(logger::debug, project, "Changing to card  $cardToShow")
            emptyStatusesCardsLayout.show(emptyStatusesCardsPanel, cardToShow)
        }
    }

    resultPanel.layout = BorderLayout()
    resultPanel.add(emptyStatusesCardsPanel,BorderLayout.CENTER)
    resultPanel.border = empty()
    resultPanel.background = listBackground()

    return resultPanel
}
