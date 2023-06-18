package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import java.awt.CardLayout
import java.awt.Cursor
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JToggleButton


class HomeButton(val project: Project, private val cardsPanel: JPanel): JToggleButton() {

    companion object{
        const val SCOPE_LINE_PANEL = "SCOPE_LINE_PANEL"
        const val HOME_PROJECT_PANEL = "HOME_PROJECT_PANEL"
    }

    init {
        icon = getMyDefaultIcon()
        selectedIcon = getMySelectedIcon()
        isSelected = service<PersistenceService>().state.homeButtonSelected
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        isOpaque = true
        isContentAreaFilled = false
        isBorderPainted = false
        border = JBUI.Borders.empty()
        background = Laf.Colors.TRANSPARENT

        changeState()

        addChangeListener{
            service<PersistenceService>().state.homeButtonSelected = isSelected
            changeState()
        }


        addMouseListener(object: MouseAdapter(){
            override fun mouseEntered(e: MouseEvent?) {
                icon = getMyHoverIcon()
            }

            override fun mouseExited(e: MouseEvent?) {
                icon = getMyDefaultIcon()
            }
        })
    }



    private fun changeState(){
        if (isSelected){
            (cardsPanel.layout as CardLayout).show(cardsPanel,HOME_PROJECT_PANEL)
            project.service<MainToolWindowCardsController>().showHome()
        }else{
            (cardsPanel.layout as CardLayout).show(cardsPanel, SCOPE_LINE_PANEL)
            project.service<MainToolWindowCardsController>().showInsights()
        }
    }



    private fun getMyDefaultIcon(): Icon {
        return if (JBColor.isBright()){
            Laf.Icons.General.HOME_DEFAULT_LIGHT
        }else{
            Laf.Icons.General.HOME_DEFAULT_DARK
        }

    }

    private fun getMySelectedIcon(): Icon {
        return if (JBColor.isBright()){
            Laf.Icons.General.HOME_SELECTED_LIGHT
        }else{
            Laf.Icons.General.HOME_SELECTED_DARK
        }

    }

    private fun getMyHoverIcon(): Icon {
        return if (JBColor.isBright()){
            Laf.Icons.General.HOME_HOVER_LIGHT
        }else{
            Laf.Icons.General.HOME_HOVER_DARK
        }
    }



    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        super.paintComponent(g)
    }
}