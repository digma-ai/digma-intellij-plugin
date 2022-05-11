@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBEmptyBorder
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.env.envCombo
import org.digma.intellij.plugin.ui.insights.insightsModel
import org.digma.intellij.plugin.ui.model.ErrorsModel
import org.digma.intellij.plugin.ui.model.InsightsModel

val errorsModel: ErrorsModel = ErrorsModel()


fun errorsPanel(project: Project): DialogPanel {


    val result = panel {
        row {
            var topLine = topLine(project, insightsModel,"Code errors")
            cell(topLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    topLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)
        row{
            var scopeLine = scopeLine(project,{ insightsModel.classAndMethod() }, ScopeLineIconProducer(insightsModel))
            cell(scopeLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    scopeLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)

    }


    result.border = JBEmptyBorder(10)

    return result
}

