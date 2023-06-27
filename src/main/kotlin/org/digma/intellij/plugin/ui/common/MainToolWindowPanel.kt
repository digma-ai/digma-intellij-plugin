package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

fun createMainToolWindowPanel(project: Project, contentPanel: ContentPanel): JPanel {

    val navigationPanel = NavigationPanel(project)
    val updatePanel = UpdateVersionPanel(project)


    val result = JPanel()
    result.isOpaque = false
    result.border = JBUI.Borders.empty()
    result.layout = BorderLayout()

    val topPanel = JPanel(BorderLayout())
    topPanel.isOpaque = false
    topPanel.border = JBUI.Borders.empty()
    topPanel.add(navigationPanel, BorderLayout.NORTH)
    topPanel.add(updatePanel, BorderLayout.CENTER)

    result.add(topPanel, BorderLayout.NORTH)
    result.add(contentPanel, BorderLayout.CENTER)

    return result
}