package org.digma.intellij.plugin.ui.common

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.ToolWindowShower
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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


        layout = GridLayout(0, 1, 5, 2)

        val settingsLabel = JLabel("Settings")
        settingsLabel.foreground = Laf.Colors.DROP_DOWN_HEADER_TEXT_COLOR
        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        topPanel.background = Laf.Colors.EDITOR_BACKGROUND
        topPanel.isOpaque = true
        topPanel.add(settingsLabel)
        add(topPanel)

        if(IDEUtilsService.getInstance(project).isJavaProject) {
            val secondPanel = Box.createHorizontalBox()
            secondPanel.background = Laf.Colors.EDITOR_BACKGROUND
            secondPanel.isOpaque = true
            secondPanel.add(Box.createHorizontalStrut(5))
            secondPanel.add(JLabel(Laf.Icons.Environment.ENVIRONMENT_HAS_NO_USAGE))
            secondPanel.add(Box.createHorizontalStrut(5))

            val togglePanel = JPanel(FlowLayout(FlowLayout.LEFT, 15, 2))
            togglePanel.background = Laf.Colors.EDITOR_BACKGROUND
            togglePanel.isOpaque = true
            togglePanel.add(JLabel("Observability"))
            val toggle = SwitchButton(40, 20, PersistenceService.getInstance().state.isAutoOtel)
            toggle.addEventSelected(object : SwitchButton.EventSwitchSelected {
                override fun onSelected(selected: Boolean) {
                    ObservabilityUtil.updateObservabilityValue(project, selected)
                }
            })
            togglePanel.add(toggle)
            secondPanel.add(togglePanel)
            add(secondPanel)
        }

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
                    MainToolWindowCardsController.getInstance(project).showWizard();
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

        val feedbackLabel = JLabel("Feedback")
        feedbackLabel.foreground = Laf.Colors.DROP_DOWN_HEADER_TEXT_COLOR
        val thirdPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        thirdPanel.background = Laf.Colors.EDITOR_BACKGROUND
        thirdPanel.isOpaque = true
        thirdPanel.add(feedbackLabel)
        add(thirdPanel)

        val fourthPanel = Box.createHorizontalBox()
        fourthPanel.background = Laf.Colors.EDITOR_BACKGROUND
        fourthPanel.isOpaque = true
        fourthPanel.add(Box.createHorizontalStrut(5))
        fourthPanel.add(JLabel(Laf.Icons.General.SLACK))
        fourthPanel.add(Box.createHorizontalStrut(20))

        val linkLabel = JLabel("Digma Channel")
        linkLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        linkLabel.addMouseListener(object : MouseAdapter() {
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
        fourthPanel.add(linkLabel)
        add(fourthPanel)
    }

}