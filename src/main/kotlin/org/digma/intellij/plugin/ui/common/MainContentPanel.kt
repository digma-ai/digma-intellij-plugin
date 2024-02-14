package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.navigation.View
import org.digma.intellij.plugin.ui.assets.AssetsPanel
import org.digma.intellij.plugin.ui.errors.errorsPanel
import org.digma.intellij.plugin.ui.insights.InsightsPanel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.tests.TestsPanel
import java.awt.CardLayout
import java.awt.Insets
import javax.swing.JPanel

class MainContentPanel(project: Project) : JPanel() {


    private val myLayout = CardLayout()

    init {
        isOpaque = false
        border = JBUI.Borders.empty()


        //the cards are managed by //todo: create a service
        layout = myLayout

        val insightsPanel = InsightsPanel(project)
        add(insightsPanel, View.Insights.cardName)
        myLayout.addLayoutComponent(insightsPanel, View.Insights.cardName)

        val assetsPanel = AssetsPanel(project)
        add(assetsPanel, View.Assets.cardName)
        myLayout.addLayoutComponent(assetsPanel, View.Assets.cardName)

        val errorsPanel = createErrorsPanel(project)
        add(errorsPanel, View.Errors.cardName)
        myLayout.addLayoutComponent(errorsPanel, View.Errors.cardName)

        val testsPanel = TestsPanel(project)
        add(testsPanel, View.Tests.cardName)
        myLayout.addLayoutComponent(testsPanel, View.Tests.cardName)

        myLayout.show(this, View.Insights.cardName)

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