@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.insights

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBEmptyBorder
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.list.insights.InsightsList
import org.digma.intellij.plugin.ui.model.insights.InsightsModel


val insightsModel: InsightsModel = InsightsModel()

fun insightsPanel(project: Project): DialogPanel {


    val result = panel {
        row {
            var topLine = topLine(project, insightsModel,"Code insights")
            cell(topLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    topLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)
        row {
            var scopeLine = scopeLine(project, { insightsModel.classAndMethod() }, ScopeLineIconProducer(insightsModel))
            cell(scopeLine)
                .horizontalAlign(HorizontalAlign.FILL)
                .onReset {
                    scopeLine.reset()
                }
        }.layout(RowLayout.PARENT_GRID)
        row {
            var insightsList = InsightsList(insightsModel.listViewItems)
            scrollCell(insightsList)
                .onReset {
                    insightsList.getModel().setListData(insightsModel.listViewItems)
                }
        }.layout(RowLayout.PARENT_GRID)
            .resizableRow()
    }


      result.border = JBEmptyBorder(10)

    return result
}

