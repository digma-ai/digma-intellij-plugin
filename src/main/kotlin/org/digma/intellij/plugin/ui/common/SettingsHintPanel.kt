package org.digma.intellij.plugin.ui.common

import com.intellij.codeInsight.hint.HintUtil
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.JPanel

private const val DIGMA_SLACK_URL = "https://join.slack.com/t/continuous-feedback/shared_invite/zt-1hk5rbjow-yXOIxyyYOLSXpCZ4RXstgA"

class SettingsHintPanel(project: Project) : JPanel() {
    private val logger = Logger.getInstance(SettingsHintPanel::class.java)

    init {
        border = HintUtil.createHintBorder()
        background = Laf.Colors.EDITOR_BACKGROUND
        isOpaque = true

        layout = GridLayout(4, 1, 5, 2)

        val settingsLabel = JLabel("Settings")
        settingsLabel.foreground = Laf.Colors.DROP_DOWN_HEADER_TEXT_COLOR
        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        topPanel.background = Laf.Colors.EDITOR_BACKGROUND
        topPanel.isOpaque = true
        topPanel.add(settingsLabel)
        add(topPanel)

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
        val toggle = SwitchButton(40, 20, PersistenceService.getInstance(project).state.isAutoOtel)
        toggle.addEventSelected(object : SwitchButton.EventSwitchSelected {
            override fun onSelected(selected: Boolean) {
                PersistenceService.getInstance(project).state.isAutoOtel = selected
            }
        })
        togglePanel.add(toggle)
        secondPanel.add(togglePanel)
        secondPanel.add(Box.createHorizontalStrut(5))
        secondPanel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, Laf.Colors.PLUGIN_BACKGROUND)
        add(secondPanel)

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
                } catch (ex: Exception) {
                    Log.log(logger::debug, "exception opening 'Digma Channel' link = {}, message: {}. ", DIGMA_SLACK_URL, ex.message)
                }
            }
        })
        fourthPanel.add(linkLabel)
        add(fourthPanel)
    }

}