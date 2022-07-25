@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.errors

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.ui.common.createTopPanel
import org.digma.intellij.plugin.ui.common.noCodeObjectWarningPanel
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.errors.ErrorsPanelList
import org.digma.intellij.plugin.ui.list.insights.PreviewList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


private const val NO_INFO_CARD_NAME="NO-INFO"
private const val LIST_CARD_NAME="LIST"
private const val PREVIEW_LIST_CARD_NAME="PREVIEW_LIST"


fun errorsPanel(project: Project): DigmaTabPanel {

    //errorsModel and insightsModel are not singletons but are single per open project.
    //they are created by the view service and live as long as the project is alive.
    //so components can bind to them, but not to members of them, the model instance is the same on but the
    //members change , like the various lists. or bind to a function of the mode like getScope.
    val errorsModel = ErrorsViewService.getInstance(project).model
    val insightsModel = InsightsViewService.getInstance(project).model

    val topPanelWrapper = createTopPanel(project, errorsModel, errorsModel::usageStatusResult)

    val errorsList = ScrollablePanelList(ErrorsPanelList(project, errorsModel.listViewItems))

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
        row{
            label("").bind(
                JLabel::getText, JLabel::setText, MutableProperty(
                    getter = { insightsModel.getPreviewListMessage() },
                    setter = {})
            )
        }
    }
    previewTitle.isOpaque = false
    val previewPanel = JPanel(BorderLayout())
    previewPanel.add(previewTitle,BorderLayout.NORTH)
    previewPanel.add(previewList,BorderLayout.CENTER)
    previewPanel.isOpaque = false


    val noInfoWarningPanel = noCodeObjectWarningPanel(errorsModel)


    //a card layout for the errorsPanelList and noInfoWarningPanel
    val errorsPanelListCardLayout = CardLayout()
    val errorsPanelListCardPanel = JPanel(errorsPanelListCardLayout)
    errorsPanelListCardPanel.isOpaque = false
    errorsPanelListCardPanel.border = empty()
    errorsPanelListCardPanel.add(errorsList, LIST_CARD_NAME)
    errorsPanelListCardPanel.add(previewPanel, PREVIEW_LIST_CARD_NAME)
    errorsPanelListCardPanel.add(noInfoWarningPanel, NO_INFO_CARD_NAME)
    errorsPanelListCardLayout.addLayoutComponent(errorsList, LIST_CARD_NAME)
    errorsPanelListCardLayout.addLayoutComponent(previewPanel, PREVIEW_LIST_CARD_NAME)
    errorsPanelListCardLayout.addLayoutComponent(noInfoWarningPanel, NO_INFO_CARD_NAME)
    errorsPanelListCardLayout.show(errorsPanelListCardPanel, NO_INFO_CARD_NAME)


    val errorsListPanel = JBPanel<JBPanel<*>>()
    errorsListPanel.layout = BorderLayout()
    errorsListPanel.isOpaque = false
    errorsListPanel.add(topPanelWrapper, BorderLayout.NORTH)
    errorsListPanel.add(errorsPanelListCardPanel, BorderLayout.CENTER)


    val errorsDetailsPanel = errorDetailsPanel(project,errorsModel)


    val cardLayout = CardLayout()
    val cardsPanel = JPanel(cardLayout)
    cardsPanel.isOpaque = false
    cardsPanel.add(errorsListPanel, ErrorsTabCard.ERRORS_LIST.name)
    cardsPanel.add(errorsDetailsPanel, ErrorsTabCard.ERROR_DETAILS.name)
    cardLayout.addLayoutComponent(errorsListPanel, ErrorsTabCard.ERRORS_LIST.name)
    cardLayout.addLayoutComponent(errorsDetailsPanel, ErrorsTabCard.ERROR_DETAILS.name)
    cardLayout.show(cardsPanel, ErrorsTabCard.ERRORS_LIST.name)

    val result = object : DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return topPanelWrapper
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return errorsList
        }

        //reset must be called from EDT
        override fun reset() {

            //for intellij DialogPanel instances call reset.
            //for others call inside SwingUtilities.invokeLater

            noInfoWarningPanel.reset()
            topPanelWrapper.reset()
            previewTitle.reset()


            errorsList.getModel().setListData(errorsModel.listViewItems)
            previewList.getModel().setListData(insightsModel.previewListViewItems)
            errorsDetailsPanel.reset()
            cardLayout.show(cardsPanel, errorsModel.card.name)

            if (errorsList.getModel().size == 0 && previewList.getModel().size > 0 && insightsModel.card == InsightsTabCard.PREVIEW) {
                errorsPanelListCardLayout.show(errorsPanelListCardPanel, PREVIEW_LIST_CARD_NAME)
            } else if (errorsList.getModel().size == 0) {
                errorsPanelListCardLayout.show(errorsPanelListCardPanel, NO_INFO_CARD_NAME)
            } else {
                errorsPanelListCardLayout.show(errorsPanelListCardPanel, LIST_CARD_NAME)
            }

            errorsPanelListCardPanel.revalidate()
            cardsPanel.revalidate()
            revalidate()
        }
    }

    result.layout = BorderLayout()
    result.add(cardsPanel,BorderLayout.CENTER)
    result.background = listBackground()
    return result
}

