package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.insights.ErrorsViewOrchestrator
import org.digma.intellij.plugin.insights.InsightsReactPanel
import org.digma.intellij.plugin.navigation.InsightsAndErrorsTabsHelper
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.errors.errorsPanel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import javax.swing.BoxLayout
import javax.swing.JPanel


private const val INSIGHTS_TAB_NAME = "Insights"
private const val ERRORS_TAB_NAME = "Errors"
private const val ERROR_DETAILS_TAB_NAME = "Error Details"
private const val INSIGHTS_TAB_INDEX = 0
private const val ERRORS_TAB_INDEX = 1

class InsightsPanel(private val project: Project) : JPanel() {

    init {
        isOpaque = false
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = empty()

        val tabbedPane = getTabsPanel()
        this.add(tabbedPane)

        project.service<InsightsAndErrorsTabsHelper>().setTabbedPane(tabbedPane,ERRORS_TAB_NAME,ERROR_DETAILS_TAB_NAME)

    }


    private fun getTabsPanel(): JBTabbedPane {
        val tabbedPane = JBTabbedPane()
        tabbedPane.isOpaque = false
        tabbedPane.border = empty()

        val insightsNewPanel = createInsightsNewPanel(project)
        insightsNewPanel.border = empty()
        tabbedPane.addTab(INSIGHTS_TAB_NAME, insightsNewPanel)
        project.service<InsightsAndErrorsTabsHelper>().setInsightsTabIndex(INSIGHTS_TAB_INDEX)


        val errorsPanel = createErrorsPanel(project)
        errorsPanel.border = empty()
        tabbedPane.addTab(ERRORS_TAB_NAME, errorsPanel)
        project.service<InsightsAndErrorsTabsHelper>().setErrorsTabIndex(ERRORS_TAB_INDEX)


        tabbedPane.addChangeListener {

            val tabsHelper = project.service<InsightsAndErrorsTabsHelper>()

            //if error detains is on and user clicks the insights tab we need to close
            // error details.
            //calling ErrorsActionsService.closeErrorDetails will in turn call
            //InsightsAndErrorsTabsHelper.errorDetailsOff which will change the tab title
            if (tabsHelper.isErrorDetailsOn() && tabbedPane.selectedIndex == INSIGHTS_TAB_INDEX) {
                project.service<ErrorsViewOrchestrator>().closeErrorDetailsInsightsTabClicked()
            }

            ActivityMonitor.getInstance(project).registerCustomEvent(
                "tabs selection-changed", mapOf(
                    "tab.name" to tabbedPane.getTitleAt(tabbedPane.selectedIndex)
                )
            )
        }

        return tabbedPane
    }

    private fun createInsightsNewPanel(project: Project): JPanel {
        return InsightsReactPanel(project)
    }


    private fun createErrorsPanel(project: Project): DigmaTabPanel {
        val errorsPanel = errorsPanel(project)
        val errorsViewService = project.getService(ErrorsViewService::class.java)
        errorsViewService.panel = errorsPanel
        return errorsPanel
    }

//    private fun createInsightsPanel(project: Project): DigmaTabPanel {
//        val insightsPanel = insightsPanel(project)
//        val insightsViewService = project.getService(InsightsViewService::class.java)
//        insightsViewService.panel = insightsPanel
//        return insightsPanel
//    }
}