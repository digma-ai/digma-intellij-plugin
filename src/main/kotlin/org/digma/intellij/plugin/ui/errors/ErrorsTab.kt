@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.errors.ErrorsPanelList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities


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


    val errorsPanelList = ScrollablePanelList(ErrorsPanelList(project, ErrorsModel.listViewItems))

    val errorsListPanel = JBPanel<JBPanel<*>>()
    errorsListPanel.layout = BorderLayout()
    errorsListPanel.isOpaque = false
    errorsListPanel.add(topPanelWrapper, BorderLayout.NORTH)
    errorsListPanel.add(errorsPanelList, BorderLayout.CENTER)


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
            return errorsPanelList
        }

        override fun reset() {
            topPanel.reset()
            SwingUtilities.invokeLater {
                errorsPanelList.getModel().setListData(ErrorsModel.listViewItems)
                errorsDetailsPanel.reset()
                cardLayout.show(cardsPanel, ErrorsModel.card.name)
                cardsPanel.revalidate()
            }
        }
    }

    result.layout = BorderLayout()
    result.add(cardsPanel,BorderLayout.CENTER)
    result.background = listBackground()
    return result
}

