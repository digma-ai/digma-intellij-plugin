package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import java.awt.CardLayout
import javax.swing.JPanel

class ContentPanel(project: Project) : JPanel() {

    private val insightsPanel = InsightsPanel(project)
    private val homePanel = HomePanel(project)


    init {
        isOpaque = false
        border = JBUI.Borders.empty()

        val cardLayout = CardLayout()
        this.layout = cardLayout
        add(insightsPanel, MainToolWindowCardsController.ContentCard.INSIGHTS.name)
        add(homePanel, MainToolWindowCardsController.ContentCard.HOME.name)

        cardLayout.addLayoutComponent(insightsPanel, MainToolWindowCardsController.ContentCard.INSIGHTS.name)
        cardLayout.addLayoutComponent(homePanel, MainToolWindowCardsController.ContentCard.HOME.name)
    }


}