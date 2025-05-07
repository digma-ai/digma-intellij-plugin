package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.recentactivity.RecentActivityToolWindowShower
import org.digma.intellij.plugin.ui.RecentActivityToolWindowCardsController
import org.digma.intellij.plugin.ui.common.create2025EAPMessagePanel
import org.digma.intellij.plugin.ui.common.is2025EAPWithJCEFRemoteEnabled
import org.digma.intellij.plugin.ui.common.sendPosthogEvent
import org.digma.intellij.plugin.ui.common.statuspanels.createAggressiveUpdatePanel
import org.digma.intellij.plugin.ui.common.statuspanels.createNoConnectionPanel
import java.awt.CardLayout
import javax.swing.JPanel

class RecentActivityToolWindowFactory : ToolWindowFactory {

    private val logger = Logger.getInstance(this::class.java)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

        Log.log(logger::info, project, "creating recent activity tool window for project  {}", project)

        //patch for jcef issue:
        //https://github.com/digma-ai/digma-intellij-plugin/issues/2668
        //https://github.com/digma-ai/digma-intellij-plugin/issues/2669
        //https://youtrack.jetbrains.com/issue/IDEA-367610/jcef-initialization-crash-in-latest-2025.1-EAP-NullPointerException-Cannot-read-field-jsQueryFunction-because-config-is-null
        if (is2025EAPWithJCEFRemoteEnabled()){
            Log.log(logger::info, project, "Jcef remote is enabled , creating user message panel", project)
            sendPosthogEvent("Recent Activity")
            val messagePanel = create2025EAPMessagePanel(project)
            val content = ContentFactory.getInstance().createContent(messagePanel, null, false)
            toolWindow.contentManager.addContent(content)
            return
        }

        //initialize AnalyticsService early so the UI can detect the connection status when created
        AnalyticsService.getInstance(project)

        RecentActivityService.getInstance(project)

        RecentActivityToolWindowShower.getInstance(project).toolWindow = toolWindow

        val recentActivityPanel = RecentActivityPanel(project)

        //mainCardsPanel contains the mainToolWindowPanel and no connection panel
        val mainCardsPanel = createCardsPanel(project, recentActivityPanel, RecentActivityService.getInstance(project))

        val content = ContentFactory.getInstance().createContent(mainCardsPanel, null, false)

        //register disposable for content
        Disposer.register(AnalyticsService.getInstance(project), content)

        toolWindow.contentManager.addContent(content)

        RecentActivityToolWindowCardsController.getInstance(project).initComponents(mainCardsPanel)

    }


    private fun createCardsPanel(project: Project, mainPanel: JPanel, parentDisposable: Disposable): JPanel {

        val cardLayout = CardLayout()
        val cardsPanel = JPanel(cardLayout)
        cardsPanel.isOpaque = false
        cardsPanel.border = JBUI.Borders.empty()

        val noConnectionPanel = createNoConnectionPanel(project, parentDisposable)
        val aggressiveUpdatePanel = createAggressiveUpdatePanel(project, parentDisposable, "recent activity")


        cardsPanel.add(mainPanel, RecentActivityToolWindowCardsController.RecentActivityWindowCard.MAIN.name)
        cardLayout.addLayoutComponent(mainPanel, RecentActivityToolWindowCardsController.RecentActivityWindowCard.MAIN.name)

        cardsPanel.add(noConnectionPanel, RecentActivityToolWindowCardsController.RecentActivityWindowCard.NO_CONNECTION.name)
        cardLayout.addLayoutComponent(noConnectionPanel, RecentActivityToolWindowCardsController.RecentActivityWindowCard.NO_CONNECTION.name)

        cardsPanel.add(aggressiveUpdatePanel, RecentActivityToolWindowCardsController.RecentActivityWindowCard.UPDATE_MODE.name)
        cardLayout.addLayoutComponent(aggressiveUpdatePanel, RecentActivityToolWindowCardsController.RecentActivityWindowCard.UPDATE_MODE.name)

        //start at MAIN
        cardLayout.show(cardsPanel, RecentActivityToolWindowCardsController.RecentActivityWindowCard.MAIN.name)

        return cardsPanel
    }

}