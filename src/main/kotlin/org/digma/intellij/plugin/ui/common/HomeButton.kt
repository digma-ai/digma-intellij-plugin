package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.navigation.HomeSwitcherService
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import java.awt.CardLayout
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JToggleButton


class HomeButton(val project: Project, private val cardsPanel: JPanel) : JToggleButton() {

    companion object {
        const val SCOPE_LINE_PANEL = "SCOPE_LINE_PANEL"
        const val HOME_PROJECT_PANEL = "HOME_PROJECT_PANEL"

        val DEFAULT_ICON = if (JBColor.isBright()) {
            Laf.Icons.General.HOME_DEFAULT_LIGHT
        } else {
            Laf.Icons.General.HOME_DEFAULT_DARK
        }

        val SELECTED_ICON = if (JBColor.isBright()) {
            Laf.Icons.General.HOME_SELECTED_LIGHT
        } else {
            Laf.Icons.General.HOME_SELECTED_DARK
        }

        val HOVER_ICON = if (JBColor.isBright()) {
            Laf.Icons.General.HOME_HOVER_LIGHT
        } else {
            Laf.Icons.General.HOME_HOVER_DARK
        }

    }

    init {

        project.service<HomeSwitcherService>().setButton(this)

        icon = DEFAULT_ICON
        selectedIcon = SELECTED_ICON
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        isOpaque = true
        isContentAreaFilled = false
        isBorderPainted = false
        border = JBUI.Borders.empty()
        background = Laf.Colors.TRANSPARENT
        isSelected = true

        //only change if home is pre-selected from persistence
        if (isSelected) changeState()

        addChangeListener {
            changeState()
        }


        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                icon = HOVER_ICON
            }

            override fun mouseExited(e: MouseEvent?) {
                icon = DEFAULT_ICON
            }
        })
    }


    private fun changeState() {
        if (isSelected) {
            (cardsPanel.layout as CardLayout).show(cardsPanel, HOME_PROJECT_PANEL)
            project.service<MainToolWindowCardsController>().showHome()
        } else {
            (cardsPanel.layout as CardLayout).show(cardsPanel, SCOPE_LINE_PANEL)
            project.service<MainToolWindowCardsController>().showInsights()
        }
        cardsPanel.revalidate()
    }

}