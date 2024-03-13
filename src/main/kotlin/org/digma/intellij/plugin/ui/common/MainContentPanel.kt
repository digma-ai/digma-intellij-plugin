package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.ui.errors.errorsPanel
import org.digma.intellij.plugin.ui.mainapp.MainAppPanel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import java.awt.CardLayout
import java.awt.Insets
import javax.swing.JPanel

class MainContentPanel(project: Project) : JPanel() {


    init {
        isOpaque = false
        border = JBUI.Borders.empty()

        val myLayout = CardLayout()

        layout = myLayout

        val mainAppPanel = MainAppPanel(project)
        add(mainAppPanel, MainContentViewSwitcher.MAIN_PANEL_CARD_NAME)
        myLayout.addLayoutComponent(mainAppPanel, MainContentViewSwitcher.MAIN_PANEL_CARD_NAME)

        val errorsPanel = createErrorsPanel(project)
        add(errorsPanel, MainContentViewSwitcher.ERRORS_PANEL_CARD_NAME)
        myLayout.addLayoutComponent(errorsPanel, MainContentViewSwitcher.ERRORS_PANEL_CARD_NAME)

        myLayout.show(this, MainContentViewSwitcher.MAIN_PANEL_CARD_NAME)

        MainContentViewSwitcher.getInstance(project).setLayout(myLayout, this)

    }


    private fun createErrorsPanel(project: Project): DigmaTabPanel {
        val errorsPanel = errorsPanel(project)
        val errorsViewService = ErrorsViewService.getInstance(project)
        errorsViewService.panel = errorsPanel
        return errorsPanel
    }

    override fun getInsets(): Insets {
        return JBUI.emptyInsets()
    }
}