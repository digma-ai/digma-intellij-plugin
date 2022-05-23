@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.insights.InsightsList
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.panels.ResettablePanel
import java.awt.BorderLayout
import javax.swing.SwingUtilities


val insightsModel: InsightsModel = InsightsModel()

fun insightsPanel(project: Project ): ResettablePanel {

    val topPanel = panel {
        indent {
            row {
                var topLine = topLine(project, insightsModel, "Code insights")
                cell(topLine)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .onReset {
                        topLine.reset()
                    }
            }
            row {
                var scopeLine = scopeLine(project, { insightsModel.classAndMethod() }, ScopeLineIconProducer(insightsModel))
                cell(scopeLine)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .onReset {
                        scopeLine.reset()
                    }
            }
        }
    }

    val insightsList = ScrollablePanelList(InsightsList(project,insightsModel.listViewItems))

    val result = object: ResettablePanel() {
        override fun reset() {
            topPanel.reset()
            SwingUtilities.invokeLater {
                insightsList.getModel().setListData(insightsModel.listViewItems)
            }
        }
    }

    result.layout = BorderLayout()
    result.add(topPanel,BorderLayout.NORTH)
    result.add(insightsList,BorderLayout.CENTER)

    return result
}