package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.launchBackground
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.TabsChanged
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.errors.errorsPanel
import org.digma.intellij.plugin.ui.insights.insightsPanel
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.*
import org.digma.intellij.plugin.ui.summary.summaryPanel
import java.util.concurrent.locks.ReentrantLock
import javax.swing.BorderFactory
import javax.swing.BoxLayout

class TabsPanel(
        project: Project
) : DigmaResettablePanel() {
    private val logger: Logger = Logger.getInstance(TabsPanel::class.java)

    private val project: Project
    private val rebuildPanelLock = ReentrantLock()
    private val tabsHelper = TabsHelper.getInstance(project)
    private val tabbedPane = JBTabbedPane()

    init {
        this.project = project
        isOpaque = false
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty()

        rebuildInBackground()

        project.messageBus.connect(project.getService(AnalyticsService::class.java))
                .subscribe(TabsChanged.TABS_CHANGED_TOPIC, TabsChanged { newTabIndex ->
                    EDT.ensureEDT {
                        if (1 == newTabIndex) {
                            tabbedPane.setTitleAt(1, TabsHelper.DETAILED_ERRORS_TAB_NAME)
                        } else {
                            tabbedPane.setTitleAt(1, TabsHelper.DEFAULT_ERRORS_TAB_NAME)
                        }
                        tabbedPane.selectedIndex = newTabIndex
                    }
                })
    }

    override fun reset() {
        rebuildInBackground()
    }

    private fun rebuildInBackground() {
        val lifetimeDefinition = LifetimeDefinition()
        lifetimeDefinition.lifetime.launchBackground {
            rebuildPanelLock.lock()
            Log.log(logger::debug, "Lock acquired for rebuild Tabs panel process.")
            try {
                rebuild()
            } finally {
                rebuildPanelLock.unlock()
                Log.log(logger::debug, "Lock released for rebuild Tabs panel process.")
                lifetimeDefinition.terminate()
            }
        }
    }

    private fun rebuild() {
        ApplicationManager.getApplication().invokeLater {
            removeExistingComponentsIfPresent()
            buildTabsPanelComponents()
            revalidate()
        }
    }

    private fun removeExistingComponentsIfPresent() {
        if (components.isNotEmpty()) {
            this.components.forEach {
                this.remove(it)
            }
        }
    }

    private fun buildTabsPanelComponents() {
        this.add(getTabsPanel())
    }

    private fun getTabsPanel(): JBTabbedPane {
        tabbedPane.isOpaque = false
        tabbedPane.border = JBUI.Borders.empty()
        val insightsPanel = createInsightsPanel(project)

        insightsPanel.border = BorderFactory.createEmptyBorder(0, 0, 0, 0) // Set the border of the panel to empty
        tabbedPane.addTab(TabsHelper.INSIGHTS_TAB_NAME, insightsPanel)

        val errorsPanel = createErrorsPanel(project)
        if (tabsHelper.isErrorDetailsOn()) {
            tabbedPane.addTab(TabsHelper.DETAILED_ERRORS_TAB_NAME, errorsPanel)
        } else {
            tabbedPane.addTab(TabsHelper.DEFAULT_ERRORS_TAB_NAME, errorsPanel)
        }
        val summaryPanel = createSummaryPanel(project);
        tabbedPane.addTab(TabsHelper.SUMMARY_TAB_NAME, summaryPanel)

        tabbedPane.border = BorderFactory.createEmptyBorder(); // Set the border of the tabbed pane to empty

        var currentTabIndex: Int

        // Add a listener to the JBTabbedPane to track tab changes
        tabbedPane.addChangeListener {
            // Get the index of the currently selected tab
            currentTabIndex = tabbedPane.selectedIndex
            // Do something with the tab indexes, such as update UI or perform logic
            tabsHelper.currentTabIndex = currentTabIndex
        }

        return tabbedPane
    }

    private fun createSummaryPanel(project: Project): DigmaTabPanel {
        val summaryPanel = summaryPanel(project)
        val summaryViewService = project.getService(SummaryViewService::class.java)
        summaryViewService.panel = summaryPanel
        return summaryPanel
    }

    private fun createErrorsPanel(project: Project): DigmaTabPanel {
        val errorsPanel = errorsPanel(project)
        val errorsViewService = project.getService(ErrorsViewService::class.java)
        errorsViewService.panel = errorsPanel
        return errorsPanel
    }

    private fun createInsightsPanel(project: Project): DigmaTabPanel {
        val insightsPanel = insightsPanel(project)
        val insightsViewService = project.getService(InsightsViewService::class.java)
        insightsViewService.panel = insightsPanel
        return insightsPanel
    }
}