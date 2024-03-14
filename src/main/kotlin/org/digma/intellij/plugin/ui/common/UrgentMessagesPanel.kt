package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.ui.common.statuspanels.createUpgradeBackendMessagePanel
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.updates.UrgentMessagesService
import java.awt.CardLayout
import java.awt.Insets

private const val MAIN_PANEL_NAME = "mainPanel"
private const val UPGRADE_BACKEND_PANEL_NAME = "upgradeBackendPanel"


class UrgentMessagesPanel(private val project: Project, mainContentPanel: MainContentPanel) : DigmaResettablePanel() {

    private val myLayout = CardLayout()

    init {
        isOpaque = false
        border = JBUI.Borders.empty()
        layout = myLayout


        add(mainContentPanel, MAIN_PANEL_NAME)
        myLayout.addLayoutComponent(mainContentPanel, MAIN_PANEL_NAME)

        val upgradeBackendPanel = createUpgradeBackendMessagePanel(project)
        add(upgradeBackendPanel, UPGRADE_BACKEND_PANEL_NAME)
        myLayout.addLayoutComponent(upgradeBackendPanel, UPGRADE_BACKEND_PANEL_NAME)

        myLayout.show(this, MAIN_PANEL_NAME)

        UrgentMessagesService.getInstance(project).setPanel(this)
    }


    override fun reset() {
        if (UrgentMessagesService.getInstance(project).shouldShowUpgradeBackendMessage()) {
            EDT.ensureEDT {
                myLayout.show(this, UPGRADE_BACKEND_PANEL_NAME)
            }
        } else {
            EDT.ensureEDT {
                myLayout.show(this, MAIN_PANEL_NAME)
            }
        }
    }


    override fun getInsets(): Insets {
        return JBUI.emptyInsets()
    }
}