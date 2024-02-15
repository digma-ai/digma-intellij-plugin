package org.digma.intellij.plugin.ui.common

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.VersionComparatorUtil
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.frameworks.QuarkusConfigureDepsPanel
import org.digma.intellij.plugin.ui.frameworks.SpringBootMicrometerConfigureDepsPanel
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

fun createMainToolWindowPanel(project: Project, contentPanel: ContentPanel): JPanel {

    val navigationPanel = NavigationPanel(project)

    val updatePanel = UpdateVersionPanel(project)

    //todo: updateIDEPanel is a notification to update intellij. relevant only for users running 2023.1 and bellow.
    // when we stop support for 2023.1 we can remove this panel.
    // updatePanel and updateIDEPanel should not be visible at the same time,
    // while updateIDEPanel is visible it prevents updatePanel from being visible. when update ide
    // button is clicked it enables updatePanel again.
    val updateIDEPanel: JPanel? = createUpdateIntellijNotificationWrapper(updatePanel,project)

    val loadStatusPanel = LoadStatusPanel(project)

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
    topPanel.add(loadStatusPanel)

    updateIDEPanel?.let {
        topPanel.add(it)
    }


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




//todo: remove when we stop support for 2023.1.
// also remove the property UpdateVersionPanel.tempEnableReset

private fun createUpdateIntellijNotificationWrapper(updatePanel: UpdateVersionPanel, project: Project): JPanel? {

    if (!shouldAddUpdateIntellijNotification()) {
        updatePanel.tempEnableReset = true
        updatePanel.reset()
        return null
    }

    ActivityMonitor.getInstance(project).registerCustomEvent(
        "show update IDE notification",
        mapOf(
            "ideVersion" to ApplicationInfo.getInstance().fullVersion
        )
    )

    return createUpdateIntellijPanel(updatePanel, project)

}

private fun createUpdateIntellijPanel(updatePanel: UpdateVersionPanel, project: Project): JPanel {

    val borderedPanel = JPanel()
    borderedPanel.layout = BoxLayout(borderedPanel, BoxLayout.Y_AXIS)
    borderedPanel.isOpaque = true
    borderedPanel.border = BorderFactory.createLineBorder(Laf.Colors.BLUE_LIGHT_SHADE, 1)

    val contentPanel = JPanel()
    contentPanel.layout = BoxLayout(contentPanel, BoxLayout.X_AXIS)
    contentPanel.background = Laf.Colors.EDITOR_BACKGROUND
    contentPanel.isOpaque = true
    contentPanel.border = JBUI.Borders.empty(4)
    contentPanel.add(JLabel(asHtml("Digma works best with IntelliJ 2023.2 and above")),BorderLayout.CENTER)

    val updateButton = UpdateActionButton()
    updateButton.addActionListener{
        BrowserUtil.browse("https://www.jetbrains.com/idea/download/")
        borderedPanel.isVisible = false
        updatePanel.tempEnableReset = true
        updatePanel.reset()

        ActivityMonitor.getInstance(project).registerCustomEvent("update IDE button clicked",
            mapOf(
                "ideVersion" to ApplicationInfo.getInstance().fullVersion
            ))
    }

    contentPanel.add(updateButton,BorderLayout.EAST)
    borderedPanel.add(contentPanel)

    return borderedPanel
}

private fun shouldAddUpdateIntellijNotification(): Boolean {
    return (SystemInfo.isMac || SystemInfo.isLinux) &&
            VersionComparatorUtil.compare("2023.2",ApplicationInfo.getInstance().fullVersion) > 0
}
