@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.insights.insightsModel
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.errors.ErrorsList
import org.digma.intellij.plugin.ui.list.insights.InsightsList
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.panels.ResettablePanel
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.SwingUtilities

val errorsModel: ErrorsModel = ErrorsModel()


fun errorsPanel(project: Project): ResettablePanel {


    val topPanel = panel {
        row {
            var topLine = topLine(project, errorsModel,"Code errors")
            cell(topLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    topLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)
        row{
            var scopeLine = scopeLine(project,{ errorsModel.classAndMethod() }, ScopeLineIconProducer(errorsModel))
            cell(scopeLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    scopeLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)

    }

    topPanel.border = JBUI.Borders.empty()
    val topPanelWrapper = Box.createHorizontalBox()
    topPanelWrapper.add(Box.createHorizontalStrut(12))
    topPanelWrapper.add(topPanel)
    topPanelWrapper.add(Box.createHorizontalStrut(8))


    val errorsList = ScrollablePanelList(ErrorsList(project, errorsModel.listViewItems))

    val result = object: ResettablePanel() {
        override fun reset() {
            topPanel.reset()
            SwingUtilities.invokeLater {
                errorsList.getModel().setListData(errorsModel.listViewItems)
            }
        }
    }

    result.layout = BorderLayout()
    result.add(topPanelWrapper,BorderLayout.NORTH)
    result.add(errorsList,BorderLayout.CENTER)

    return result
}

