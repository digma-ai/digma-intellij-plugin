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
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.*


fun insightsPanel(project: Project ): DigmaTabPanel {

    val topPanel = panel {
            row {
                val topLine = topLine(project, InsightsModel, "Code insights")
                cell(topLine)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .onReset {
                        topLine.reset()
                    }
            }
            row {
                val scopeLine = scopeLine(project, { InsightsModel.scope.getScope() }, ScopeLineIconProducer(InsightsModel))
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




    val insightsList = ScrollablePanelList(InsightsList(project,InsightsModel.listViewItems))
    val previewList = ScrollablePanelList(PreviewList(project,InsightsModel.previewListViewItems))

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


    val previewPanel = JPanel(BorderLayout())
    previewPanel.add(previewTitle,BorderLayout.NORTH)
    previewPanel.add(previewList,BorderLayout.CENTER)

    val cardLayout = CardLayout()
    val cardsPanel = JPanel(cardLayout)
    cardsPanel.add(insightsList, InsightsTabCard.INSIGHTS.name)
    cardsPanel.add(previewPanel, InsightsTabCard.PREVIEW.name)
    cardLayout.addLayoutComponent(insightsList, InsightsTabCard.INSIGHTS.name)
    cardLayout.addLayoutComponent(previewPanel, InsightsTabCard.PREVIEW.name)
    cardLayout.show(cardsPanel,InsightsModel.card.name)

    val result = object: DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return topPanel
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return if (InsightsModel.card.name == InsightsTabCard.INSIGHTS.name){
                insightsList
            }else{
                previewList
            }
        }

        override fun reset() {
            topPanel.reset()
            previewTitle.reset()
            SwingUtilities.invokeLater {
                insightsList.getModel().setListData(InsightsModel.listViewItems)
                previewList.getModel().setListData(InsightsModel.previewListViewItems)
                cardLayout.show(cardsPanel,InsightsModel.card.name)
                cardsPanel.revalidate()
            }
        }
    }

    result.layout = BorderLayout()
    result.add(topPanelWrapper,BorderLayout.NORTH)
    result.add(cardsPanel,BorderLayout.CENTER)

    return result
}