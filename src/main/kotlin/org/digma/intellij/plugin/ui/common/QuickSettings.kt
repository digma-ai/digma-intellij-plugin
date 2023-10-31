package org.digma.intellij.plugin.ui.common

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.documentation.DocumentationService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityUpdater
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.Collections
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


private const val DIGMA_SLACK_URL = "https://join.slack.com/t/continuous-feedback/shared_invite/zt-1hk5rbjow-yXOIxyyYOLSXpCZ4RXstgA"


class QuickSettingsButton(private val project: Project): JLabel(Laf.Icons.Insight.THREE_DOTS, SwingConstants.RIGHT){

    init {
        horizontalAlignment = SwingConstants.RIGHT
        verticalAlignment = SwingConstants.TOP
        isOpaque = false
        border = JBUI.Borders.empty(2, 5)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                showSettingsMessage()
            }

            override fun mouseExited(e: MouseEvent?) {
                //nothing to do
            }
            override fun mousePressed(e: MouseEvent?) {
                //nothing to do
            }
        })
    }


    private fun showSettingsMessage() {
        HintManager.getInstance().showHint(SettingsHintPanel(project), RelativePoint.getSouthWestOf(this), HintManager.HIDE_BY_ESCAPE, 60000)
    }

}





class SettingsHintPanel(project: Project) : JPanel() {
    private val logger = Logger.getInstance(SettingsHintPanel::class.java)

    init {
        border = HintUtil.createHintBorder()
        background = Laf.Colors.EDITOR_BACKGROUND
        isOpaque = true


        layout = GridLayout(0, 1, 5, 5)

        val settingsLabel = JLabel("Settings")
        settingsLabel.foreground = Laf.Colors.DROP_DOWN_HEADER_TEXT_COLOR
        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        topPanel.background = Laf.Colors.EDITOR_BACKGROUND
        topPanel.isOpaque = true
        topPanel.add(settingsLabel)
        add(topPanel)



        addLocalEngine(project)
        addObservability(project)
        addOnboarding(project)
        addTroubleshooting(project)
        addOverview(project)
        addFeedback(project)
    }



    private fun addTroubleshooting(project: Project) {
        val troubleshootingPanel = Box.createHorizontalBox()
        troubleshootingPanel.background = Laf.Colors.EDITOR_BACKGROUND
        troubleshootingPanel.isOpaque = true
        troubleshootingPanel.add(Box.createHorizontalStrut(5))
        troubleshootingPanel.add(JLabel(Laf.Icons.Common.Mascot16))
        troubleshootingPanel.add(Box.createHorizontalStrut(15))

        val troubleshootingLinkLabel = JLabel("Troubleshooting")
        troubleshootingLinkLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        troubleshootingLinkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    ActivityMonitor.getInstance(project).registerCustomEvent("troubleshooting link clicked", Collections.singletonMap("origin", "quick settings"))
                    MainToolWindowCardsController.getInstance(project).showTroubleshooting()
                    ToolWindowShower.getInstance(project).showToolWindow()
                    HintManager.getInstance().hideAllHints()
                } catch (ex: Exception) {
                    Log.log(logger::debug, "exception opening 'Troubleshooting' message: {}. ", ex.message)
                }
            }
        })

        troubleshootingPanel.add(troubleshootingLinkLabel)
        troubleshootingPanel.add(Box.createHorizontalStrut(5))
        troubleshootingPanel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, Laf.Colors.PLUGIN_BACKGROUND)
        add(troubleshootingPanel)
    }


    private fun addFeedback(project: Project) {
        val feedbackLabel = JLabel("Feedback")
        feedbackLabel.foreground = Laf.Colors.DROP_DOWN_HEADER_TEXT_COLOR
        val thirdPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        thirdPanel.background = Laf.Colors.EDITOR_BACKGROUND
        thirdPanel.isOpaque = true
        thirdPanel.add(feedbackLabel)
        add(thirdPanel)


        addDigmaChannel(project)


    }

    private fun addDigmaChannel(project: Project) {
        val digmaChannelPanel = Box.createHorizontalBox()
        digmaChannelPanel.background = Laf.Colors.EDITOR_BACKGROUND
        digmaChannelPanel.isOpaque = true
        digmaChannelPanel.add(Box.createHorizontalStrut(5))
        digmaChannelPanel.add(JLabel(Laf.Icons.General.SLACK))
        digmaChannelPanel.add(Box.createHorizontalStrut(20))

        val digmaChannelLabel = JLabel("Digma Channel")
        digmaChannelLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        digmaChannelLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    ApplicationManager.getApplication().invokeLater {
                        BrowserUtil.browse(DIGMA_SLACK_URL, project)
                    }
                    HintManager.getInstance().hideAllHints()
                } catch (ex: Exception) {
                    Log.log(logger::debug, "exception opening 'Digma Channel' link = {}, message: {}. ", DIGMA_SLACK_URL, ex.message)
                }
            }
        })
        digmaChannelPanel.add(digmaChannelLabel)
        add(digmaChannelPanel)
    }

    private fun addOverview(project: Project) {
        val panel = Box.createHorizontalBox()
        panel.background = Laf.Colors.EDITOR_BACKGROUND
        panel.isOpaque = true
        panel.add(Box.createHorizontalStrut(5))
        panel.add(JLabel(Laf.Icons.Common.Mascot16))
        panel.add(Box.createHorizontalStrut(15))

        val linkLabel = JLabel("Insights Overview")
        linkLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        linkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    DocumentationService.getInstance(project).openDocumentation("environment-types")
                } catch (ex: Exception) {
                    Log.log(logger::debug, "exception opening 'Overview' message: {}. ", ex.message)
                }
            }
        })

        panel.add(linkLabel)
        panel.add(Box.createHorizontalStrut(5))
        panel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, Laf.Colors.PLUGIN_BACKGROUND)
        add(panel)
    }

    private fun addOnboarding(project: Project) {
        val onboardingPanel = Box.createHorizontalBox()
        onboardingPanel.background = Laf.Colors.EDITOR_BACKGROUND
        onboardingPanel.isOpaque = true
        onboardingPanel.add(Box.createHorizontalStrut(5))
        onboardingPanel.add(JLabel(Laf.Icons.Common.Mascot16))
        onboardingPanel.add(Box.createHorizontalStrut(15))

        val onboardingLinkLabel = JLabel("Onboarding Digma")
        onboardingLinkLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        onboardingLinkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    MainToolWindowCardsController.getInstance(project).showWizard(true)
                    ToolWindowShower.getInstance(project).showToolWindow()
                    HintManager.getInstance().hideAllHints()
                } catch (ex: Exception) {
                    Log.log(logger::debug, "exception opening 'Onboarding Digma' message: {}. ", ex.message)
                }
            }
        })

        onboardingPanel.add(onboardingLinkLabel)
        onboardingPanel.add(Box.createHorizontalStrut(5))
        onboardingPanel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, Laf.Colors.PLUGIN_BACKGROUND)
        add(onboardingPanel)
    }




    private fun addObservability(project: Project) {
        if(IDEUtilsService.getInstance(project).isJavaProject) {
            val observabilityPanel = Box.createHorizontalBox()
            observabilityPanel.background = Laf.Colors.EDITOR_BACKGROUND
            observabilityPanel.isOpaque = true
            observabilityPanel.add(Box.createHorizontalStrut(5))
            observabilityPanel.add(JLabel(Laf.Icons.Environment.ENVIRONMENT_HAS_NO_USAGE))
            observabilityPanel.add(Box.createHorizontalStrut(5))

            val togglePanel = JPanel(FlowLayout(FlowLayout.LEFT, 15, 2))
            togglePanel.background = Laf.Colors.EDITOR_BACKGROUND
            togglePanel.isOpaque = true
            togglePanel.add(JLabel("Observability"))
            val toggle = SwitchButton(40, 20, PersistenceService.getInstance().state.isAutoOtel)
            toggle.addEventSelected(object : SwitchButton.EventSwitchSelected {
                override fun onSelected(selected: Boolean) {
                    ObservabilityUtil.updateObservabilityValue(project, selected)
                    project.service<RecentActivityUpdater>().updateSetObservability(selected)
                }
            })
            togglePanel.add(toggle)
            observabilityPanel.add(togglePanel)
            add(observabilityPanel)
        }
    }


    private fun addLocalEngine(project: Project) {
        if (service<DockerService>().isEngineInstalled()) {

            val localeEnginePanel = Box.createHorizontalBox()
            localeEnginePanel.background = Laf.Colors.EDITOR_BACKGROUND
            localeEnginePanel.isOpaque = true

            if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                localeEnginePanel.add(Box.createHorizontalStrut(5))
                localeEnginePanel.add(JLabel(Laf.Icons.General.ACTIVE_GREEN))
            }

            localeEnginePanel.add(Box.createHorizontalStrut(15))
            val localeEngineLabel = JLabel("Local Engine")
            localeEngineLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            localeEngineLabel.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    try {
                        MainToolWindowCardsController.getInstance(project).showWizard(false)
                        ToolWindowShower.getInstance(project).showToolWindow()
                        HintManager.getInstance().hideAllHints()
                    } catch (ex: Exception) {
                        Log.log(logger::debug, "exception opening 'Local Engine' message: {}. ", ex.message)
                    }
                }
            })

            localeEnginePanel.add(localeEngineLabel)
            localeEnginePanel.add(Box.createHorizontalStrut(5))
            localeEnginePanel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, Laf.Colors.PLUGIN_BACKGROUND)
            add(localeEnginePanel)
        }
    }

}