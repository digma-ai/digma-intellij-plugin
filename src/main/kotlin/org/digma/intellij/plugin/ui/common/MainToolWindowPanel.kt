package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.ui.frameworks.QuarkusConfigureDepsPanel
import org.digma.intellij.plugin.ui.frameworks.SpringBootMicrometerConfigureDepsPanel
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel

fun createMainToolWindowPanel(project: Project, contentPanel: ContentPanel): JPanel {

    val navigationPanel = NavigationPanel(project)
    val updatePanel = UpdateVersionPanel(project)
    val quarkusConfigureDepsPanel: JPanel? =
        if (IDEUtilsService.getInstance(project).isJavaProject()) {
            QuarkusConfigureDepsPanel(project)
        } else {
            null
        }

    val springBootConfigureDepsPanel: JPanel? =
        if (IDEUtilsService.getInstance(project).isJavaProject()) {
            SpringBootMicrometerConfigureDepsPanel(project)
        } else {
            null
        }

    val result = JPanel()
    result.isOpaque = false
    result.border = JBUI.Borders.empty()
    result.layout = BorderLayout()

    val topPanel = JPanel()
    topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
    topPanel.isOpaque = false
    topPanel.border = JBUI.Borders.empty()
    topPanel.add(navigationPanel)
    topPanel.add(updatePanel)

    quarkusConfigureDepsPanel?.let {
        topPanel.add(it)
    }

    springBootConfigureDepsPanel?.let {
        topPanel.add(it)
    }

    result.add(topPanel, BorderLayout.NORTH)
    result.add(contentPanel, BorderLayout.CENTER)

    return result
}