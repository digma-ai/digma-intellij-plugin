package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.CardLayout

class ContentPanel(project: Project) : DigmaResettablePanel() {

    private val insightsPanel = TabsPanel(project)
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
//        if (service<PersistenceService>().state.homeButtonSelected){
//            cardLayout.show(this, MainToolWindowCardsController.ContentCard.HOME.name)
//        }else {
//            cardLayout.show(this, MainToolWindowCardsController.ContentCard.INSIGHTS.name)
//        }

        //project.service<HomeSwitcherService>().switchToHome()
    }



    override fun reset() {

    }


}