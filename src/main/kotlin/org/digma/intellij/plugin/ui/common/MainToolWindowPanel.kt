package org.digma.intellij.plugin.ui.common

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.VersionComparatorUtil
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.ui.frameworks.QuarkusConfigureDepsPanel
import org.digma.intellij.plugin.ui.frameworks.SpringBootMicrometerConfigureDepsPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

fun createMainToolWindowPanel(project: Project, contentPanel: ContentPanel): JPanel {

    val navigationPanel = NavigationPanel(project)

    //todo: creating a notification to update intellij. relevant only for users running 2023.1 and bellow.
    // when we stop support for 2023.1 we can remove this wrapper panel and use original updatePanel
    //val updatePanel = UpdateVersionPanel(project)
    val updatePanel = createUpdateIntellijNotificationWrapper(UpdateVersionPanel(project))

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




//todo: remove when we stop support for 2023.1.
// also remove the property UpdateVersionPanel.tempEnableReset

private const val ORG_UPDATE_PANEL = "ORG_UPDATE_PANEL"
private const val UPDATE_INTELLIJ_PANEL = "UPDATE_INTELLIJ_PANEL"

private fun createUpdateIntellijNotificationWrapper(updatePanel: UpdateVersionPanel):JPanel{

    if (!shouldAddUpdateIntellijNotification()){
        updatePanel.tempEnableReset = true
        updatePanel.reset()
        return updatePanel
    }

    val cardLayout = CardLayout()
    val wrapper = JPanel(cardLayout)

    val updateIntellijPanel = createUpdateIntellijPanel(cardLayout,wrapper,updatePanel)


    wrapper.border = JBUI.Borders.empty(4)
    wrapper.add(updatePanel, ORG_UPDATE_PANEL)
    wrapper.add(updateIntellijPanel, UPDATE_INTELLIJ_PANEL)
    cardLayout.addLayoutComponent(updatePanel, ORG_UPDATE_PANEL)
    cardLayout.addLayoutComponent(updateIntellijPanel, UPDATE_INTELLIJ_PANEL)
    cardLayout.show(wrapper, UPDATE_INTELLIJ_PANEL)

    return wrapper

}

fun createUpdateIntellijPanel(cardLayout: CardLayout, wrapper: JPanel, updatePanel: UpdateVersionPanel): JPanel {

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
        cardLayout.show(wrapper, ORG_UPDATE_PANEL)
        updatePanel.tempEnableReset = true
        updatePanel.reset()
    }
    contentPanel.add(updateButton,BorderLayout.EAST)

    borderedPanel.add(contentPanel)
    return borderedPanel
}

fun shouldAddUpdateIntellijNotification(): Boolean {

    return (SystemInfo.isMac || SystemInfo.isLinux) &&
            VersionComparatorUtil.compare("2023.2",ApplicationInfo.getInstance().fullVersion) > 0
}
