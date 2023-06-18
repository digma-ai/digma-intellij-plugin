package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import org.digma.intellij.plugin.analytics.TabsChanged
import org.digma.intellij.plugin.log.Log

class TabsHelper(val project: Project) {
    private val logger: Logger = Logger.getInstance(TabsHelper::class.java)

    var currentTabIndex = 0

    private var visibleTabBeforeErrorDetails: Int? = null
    private var errorDetailsOn = false

    companion object {
        const val INSIGHTS_TAB_NAME = "Insights"
        const val DEFAULT_ERRORS_TAB_NAME = "Errors"
        const val DETAILED_ERRORS_TAB_NAME = "Error Details"
        const val DASHBOARD_TAB_NAME = "Dashboard"
        const val ASSETS_TAB_NAME = "Assets"

        @JvmStatic
        fun getInstance(project: Project): TabsHelper {
            return project.getService(TabsHelper::class.java)
        }
    }

    fun isInsightsTab(content: Content?): Boolean {
        return content != null && content.tabName.equals(INSIGHTS_TAB_NAME, ignoreCase = true)
    }

    fun isErrorsTab(content: Content?): Boolean {
        return content != null && content.tabName.equals(DEFAULT_ERRORS_TAB_NAME, ignoreCase = true)
    }

    fun isSummaryTab(content: Content?): Boolean {
        return content != null && content.tabName.equals(DASHBOARD_TAB_NAME, ignoreCase = true)
    }


    fun showingErrorDetails() {
        visibleTabBeforeErrorDetails = currentTabIndex
    }

    fun errorDetailsClosed() {
        visibleTabBeforeErrorDetails?.let {
            notifyTabChanged(it)
        }
        visibleTabBeforeErrorDetails = null
    }

    //todo: a weird way to change tabs, this method does not notify about tab changed, it requests the tabbed pane to change a tab.
    // the only listener for this event is the TabsPanel and it changed tab to the requested index.
    // this class should have method like showErrorsTab, showInsightsTab etc. that should call TabsPanel to change tab,
    // tabs panel may fire en event that tab was changed.
    fun notifyTabChanged(newTabIndex: Int) {
        Log.log(logger::info, "Firing TabChanged event for {}", newTabIndex)
        if (project.isDisposed) {
            return
        }
        val publisher = project.messageBus.syncPublisher(TabsChanged.TABS_CHANGED_TOPIC)
        publisher.activeTabIndexChanged(newTabIndex)
    }

    fun errorDetailsOn() {
        errorDetailsOn = true
        notifyTabChanged(-1)
    }

    fun errorDetailsOff() {
        errorDetailsOn = false
    }

    fun isErrorDetailsOn(): Boolean {
        return errorDetailsOn
    }

}