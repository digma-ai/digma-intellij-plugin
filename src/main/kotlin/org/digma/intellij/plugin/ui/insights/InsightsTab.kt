@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.insights

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.PropertyBinding
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.ScopeLineIconProducer
import org.digma.intellij.plugin.ui.common.scopeLine
import org.digma.intellij.plugin.ui.common.topLine
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.insights.InsightsList
import org.digma.intellij.plugin.ui.list.insights.PreviewList
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.digma.intellij.plugin.ui.panels.ResettablePanel
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities


val insightsModel: InsightsModel = InsightsModel()

fun insightsPanel(project: Project ): ResettablePanel {

    val topPanel = panel {
            row {
                val topLine = topLine(project, insightsModel, "Code insights")
                cell(topLine)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .onReset {
                        topLine.reset()
                    }
            }
            row {
                val scopeLine = scopeLine(project, { insightsModel.scope.getScope() }, ScopeLineIconProducer(insightsModel))
                cell(scopeLine)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .onReset {
                        scopeLine.reset()
                    }
            }
    }

    topPanel.border = JBUI.Borders.empty()
    val topPanelWrapper = Box.createHorizontalBox()
    topPanelWrapper.add(Box.createHorizontalStrut(12))
    topPanelWrapper.add(topPanel)
    topPanelWrapper.add(Box.createHorizontalStrut(8))




    val insightsList = ScrollablePanelList(InsightsList(project,insightsModel.listViewItems))
    val previewList = ScrollablePanelList(PreviewList(project,insightsModel.previewListViewItems))

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
                    get = { insightsModel.getPreviewListMessage() },
                    set = {})
            )
        }

    }


    val previewPanel = JPanel(BorderLayout())
    previewPanel.add(previewTitle,BorderLayout.NORTH)
    previewPanel.add(previewList,BorderLayout.CENTER)

    val cardLayout = CardLayout()
    val cardsPanel = JPanel(cardLayout)
    cardsPanel.add(insightsList, InsightsTabCard.INSIGHTS.name)
    cardsPanel.add(previewPanel, InsightsTabCard.PREVIEW.name)
    cardLayout.addLayoutComponent(insightsList, InsightsTabCard.INSIGHTS.name)
    cardLayout.addLayoutComponent(previewPanel, InsightsTabCard.PREVIEW.name)
    cardLayout.show(cardsPanel,insightsModel.card.name)

    val result = object: ResettablePanel() {
        override fun reset() {
            topPanel.reset()
            previewTitle.reset()
            SwingUtilities.invokeLater {
                insightsList.getModel().setListData(insightsModel.listViewItems)
                previewList.getModel().setListData(insightsModel.previewListViewItems)
                cardLayout.show(cardsPanel,insightsModel.card.name)
                cardsPanel.revalidate()
            }
        }
    }

    result.layout = BorderLayout()
    result.add(topPanelWrapper,BorderLayout.NORTH)
    result.add(cardsPanel,BorderLayout.CENTER)

    return result
}