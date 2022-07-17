@file:Suppress("UnstableApiUsage")

package org.digma.intellij.plugin.ui.insights

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.common.createTopPanel
import org.digma.intellij.plugin.ui.common.noCodeObjectWarningPanel
import org.digma.intellij.plugin.ui.list.ScrollablePanelList
import org.digma.intellij.plugin.ui.list.insights.InsightsList
import org.digma.intellij.plugin.ui.list.insights.PreviewList
import org.digma.intellij.plugin.ui.list.listBackground
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities


private const val NO_INFO_CARD_NAME="NO-INFO"

fun insightsPanel(project: Project ): DigmaTabPanel {

    val topPanelWrapper = createTopPanel(project,InsightsModel,"Code insights")


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
                JLabel::getText, JLabel::setText, MutableProperty(
                    getter = { InsightsModel.getPreviewListMessage() },
                    setter = {})
            )
        }
    }

    previewTitle.isOpaque = false

    val previewPanel = JPanel(BorderLayout())
    previewPanel.add(previewTitle,BorderLayout.NORTH)
    previewPanel.add(previewList,BorderLayout.CENTER)
    previewPanel.isOpaque = false


    val noInfoWarningPanel = noCodeObjectWarningPanel(InsightsModel)

    val cardLayout = CardLayout()
    val cardsPanel = JPanel(cardLayout)
    cardsPanel.isOpaque = false
    cardsPanel.add(insightsList, InsightsTabCard.INSIGHTS.name)
    cardsPanel.add(previewPanel, InsightsTabCard.PREVIEW.name)
    cardsPanel.add(noInfoWarningPanel,NO_INFO_CARD_NAME )
    cardLayout.addLayoutComponent(insightsList, InsightsTabCard.INSIGHTS.name)
    cardLayout.addLayoutComponent(previewPanel, InsightsTabCard.PREVIEW.name)
    cardLayout.addLayoutComponent(noInfoWarningPanel, NO_INFO_CARD_NAME)
    cardLayout.show(cardsPanel,InsightsModel.card.name)

    val result = object: DigmaTabPanel() {
        override fun getPreferredFocusableComponent(): JComponent {
            return topPanelWrapper
        }

        override fun getPreferredFocusedComponent(): JComponent {
            return if (InsightsModel.card.name == InsightsTabCard.INSIGHTS.name){
                insightsList
            }else{
                previewList
            }
        }

        override fun reset() {

            //for intellij DialogPanel instances call reset.
            //for others call inside SwingUtilities.invokeLater

            noInfoWarningPanel.reset()
            topPanelWrapper.reset()
            previewTitle.reset()


            SwingUtilities.invokeLater {
                insightsList.getModel().setListData(InsightsModel.listViewItems)
                previewList.getModel().setListData(InsightsModel.previewListViewItems)

                if (insightsList.getModel().size == 0 && InsightsModel.card.equals(InsightsTabCard.INSIGHTS)){
                    cardLayout.show(cardsPanel,NO_INFO_CARD_NAME)
                }else if (previewList.getModel().size == 0 && InsightsModel.card.equals(InsightsTabCard.PREVIEW)){
                    cardLayout.show(cardsPanel,NO_INFO_CARD_NAME)
                }else{
                    cardLayout.show(cardsPanel,InsightsModel.card.name)
                }

                cardsPanel.revalidate()
            }
        }
    }

    result.layout = BorderLayout()
    result.add(topPanelWrapper,BorderLayout.NORTH)
    result.add(cardsPanel,BorderLayout.CENTER)
    result.background = listBackground()

    return result
}






