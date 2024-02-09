package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.insights.InsightsReactPanel
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.navigation.View
import org.digma.intellij.plugin.ui.assets.AssetsPanel
import org.digma.intellij.plugin.ui.errors.errorsPanel
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

        val insightsPanel = InsightsReactPanel(project)
        add(insightsPanel, View.Insights.id)
        myLayout.addLayoutComponent(insightsPanel, View.Insights.id)

        val assetsPanel = AssetsPanel(project)
        add(assetsPanel, View.Assets.id)
        myLayout.addLayoutComponent(assetsPanel, View.Assets.id)

        val errorsPanel = createErrorsPanel(project)
        add(errorsPanel, View.Errors.id)
        myLayout.addLayoutComponent(errorsPanel, View.Errors.id)

        val testsPanel = TestsPanel(project)
        add(testsPanel, View.Tests.id)
        myLayout.addLayoutComponent(testsPanel, View.Tests.id)


        myLayout.show(this, View.Insights.id)

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