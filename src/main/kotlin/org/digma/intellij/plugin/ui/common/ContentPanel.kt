package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import java.awt.CardLayout
import javax.swing.JPanel

class ContentPanel(project: Project) : JPanel() {

    init {
        isOpaque = false
        border = JBUI.Borders.empty()

        //the cards are managed by MainToolWindowCardsController

        val insightsPanel = InsightsPanel(project)
        val homePanel = HomePanel(project)
        val cardLayout = CardLayout()
        this.layout = cardLayout
        add(insightsPanel, MainToolWindowCardsController.ContentCard.INSIGHTS.name)
        add(homePanel, MainToolWindowCardsController.ContentCard.HOME.name)
        cardLayout.addLayoutComponent(insightsPanel, MainToolWindowCardsController.ContentCard.INSIGHTS.name)
        cardLayout.addLayoutComponent(homePanel, MainToolWindowCardsController.ContentCard.HOME.name)

        //start at home
        cardLayout.show(this,MainToolWindowCardsController.ContentCard.HOME.name)
    }


}