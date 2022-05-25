@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.insights.insightsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.panels.ResettablePanel
import java.awt.BorderLayout

val errorsModel: ErrorsModel = ErrorsModel()


fun errorsPanel(project: Project): ResettablePanel {


    val topPanel = panel {
        row {
            var topLine = topLine(project, insightsModel,"Code errors")
            cell(topLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    topLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)
        row{
            var scopeLine = scopeLine(project,{ errorsModel.classAndMethod() }, ScopeLineIconProducer(insightsModel))
            cell(scopeLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    scopeLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)

    }

    val result = object: ResettablePanel() {
        override fun reset() {
            topPanel.reset()
        }
    }

    result.layout = BorderLayout()
    result.add(topPanel, BorderLayout.CENTER)

    return result
}

