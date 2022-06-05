@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.errors.ErrorsPanelList
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.panels.ResettablePanel
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.SwingUtilities

val errorsModel: ErrorsModel = ErrorsModel()


fun errorsPanel(project: Project): ResettablePanel {


    val topPanel = panel {
        row {
            val topLine = topLine(project, errorsModel, "Code errors")
            cell(topLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    topLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)
        row {
            val scopeLine = scopeLine(project, { errorsModel.getScope() }, ScopeLineIconProducer(errorsModel))
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


    val errorsPanelList = ScrollablePanelList(ErrorsPanelList(project, errorsModel.listViewItems))

    val result = object : ResettablePanel() {
        override fun reset() {
            topPanel.reset()
            SwingUtilities.invokeLater {
                errorsPanelList.getModel().setListData(errorsModel.listViewItems)
            }
        }
    }

    result.layout = BorderLayout()
    result.add(topPanelWrapper, BorderLayout.NORTH)
    result.add(errorsPanelList, BorderLayout.CENTER)

    return result
}

