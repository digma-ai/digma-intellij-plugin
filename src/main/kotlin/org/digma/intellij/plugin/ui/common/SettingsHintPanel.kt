package org.digma.intellij.plugin.ui.common

import com.intellij.codeInsight.hint.HintUtil
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.persistence.PersistenceService
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.JPanel

class SettingsHintPanel(project: Project): JPanel() {

    init {
        border = HintUtil.createHintBorder()
        background = HintUtil.getInformationColor()
        isOpaque = true

        layout = GridLayout(2,1,5,5)

        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        topPanel.background = HintUtil.getInformationColor()
        topPanel.isOpaque = true
        topPanel.add(JLabel("Settings"))
        add(topPanel)

        val secondPanel = Box.createHorizontalBox()
        secondPanel.background = HintUtil.getInformationColor()
        secondPanel.isOpaque = true
        secondPanel.add(Box.createHorizontalStrut(5))
        secondPanel.add(JLabel(Laf.Icons.Environment.ENVIRONMENT_HAS_NO_USAGE))
        secondPanel.add(Box.createHorizontalStrut(10))

        val togglePanel = JPanel(FlowLayout(FlowLayout.LEFT,15,5))
        togglePanel.background = HintUtil.getInformationColor()
        togglePanel.isOpaque = true
        togglePanel.add(JLabel("Observability"))
        val toggle = SwitchButton(50,25, PersistenceService.getInstance(project).state.isAutoOtel)
        toggle.addEventSelected(object: SwitchButton.EventSwitchSelected{
            override fun onSelected(selected: Boolean) {
                PersistenceService.getInstance(project).state.isAutoOtel = selected
            }
        })
        togglePanel.add(toggle)
        secondPanel.add(togglePanel)
        secondPanel.add(Box.createHorizontalStrut(5))

        add(secondPanel)
    }

}