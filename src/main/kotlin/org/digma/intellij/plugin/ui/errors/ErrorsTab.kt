@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.errors

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.PropertyBinding
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.noCodeObjectWarningPanel
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.errors.ErrorsPanelList
import org.digma.intellij.plugin.ui.list.insights.PreviewList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.*


private const val NO_INFO_CARD_NAME="NO-INFO"
private const val LIST_CARD_NAME="LIST"
private const val PREVIEW_LIST_CARD_NAME="PREVIEW_LIST"


fun errorsPanel(project: Project): DigmaTabPanel {


    val topPanel = panel {
        row {
            val topLine = topLine(project, ErrorsModel, "Code errors")
            topLine.isOpaque = false
            cell(topLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    topLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)
        row {
            val scopeLine = scopeLine(project, { ErrorsModel.getScope() }, ScopeLineIconProducer(ErrorsModel))
            scopeLine.isOpaque = false
            cell(scopeLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    scopeLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)

    }

    topPanel.border = empty()
    topPanel.isOpaque = false
    val topPanelWrapper = Box.createHorizontalBox()
    topPanelWrapper.isOpaque = false
    topPanelWrapper.add(Box.createHorizontalStrut(12))
    topPanelWrapper.add(topPanel)
    topPanelWrapper.add(Box.createHorizontalStrut(8))


    val errorsList = ScrollablePanelList(ErrorsPanelList(project, ErrorsModel.listViewItems))

    val previewList = ScrollablePanelList(PreviewList(project, InsightsModel.previewListViewItems))
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
                JLabel::getText, JLabel::setText, PropertyBinding(
                    get = { InsightsModel.getPreviewListMessage() },
                    set = {})
            )
        }
    }
    previewTitle.isOpaque = false
    val previewPanel = JPanel(BorderLayout())
    previewPanel.add(previewTitle,BorderLayout.NORTH)
    previewPanel.add(previewList,BorderLayout.CENTER)
    previewPanel.isOpaque = false


    val noInfoWarningPanel = noCodeObjectWarningPanel("No errors about this code object yet.")

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


    val errorsDetailsPanel = errorDetailsPanel(project,ErrorsModel)


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
            return topPanel
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return errorsList
        }

        override fun reset() {
            topPanel.reset()
            previewTitle.reset()
            SwingUtilities.invokeLater {
                errorsList.getModel().setListData(ErrorsModel.listViewItems)
                previewList.getModel().setListData(InsightsModel.previewListViewItems)
                errorsDetailsPanel.reset()
                cardLayout.show(cardsPanel, ErrorsModel.card.name)

                if (errorsList.getModel().size == 0 && previewList.getModel().size > 0 && InsightsModel.card == InsightsTabCard.PREVIEW){
                    errorsPanelListCardLayout.show(errorsPanelListCardPanel, PREVIEW_LIST_CARD_NAME)
                }else if(errorsList.getModel().size == 0){
                    errorsPanelListCardLayout.show(errorsPanelListCardPanel, NO_INFO_CARD_NAME)
                }else{
                    errorsPanelListCardLayout.show(errorsPanelListCardPanel, LIST_CARD_NAME)
                }

                errorsPanelListCardPanel.revalidate()
                cardsPanel.revalidate()
            }
        }
    }

    result.layout = BorderLayout()
    result.add(cardsPanel,BorderLayout.CENTER)
    result.background = listBackground()
    return result
}

