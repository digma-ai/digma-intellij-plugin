package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content

class ToolWindowTabsHelper(val project: Project) {

    lateinit var toolWindow: ToolWindow

    lateinit var insightsContent: Content
    lateinit var errorsContent: Content

    private var visibleTabBeforeErrorDetails: Content? = null
    private var errorDetailsOn = false

    companion object {
        const val INSIGHTS_TAB_NAME = "Insights"
        const val ERRORS_TAB_NAME = "Errors"
        const val SUMMARY_TAB_NAME = "Summary"

        @JvmStatic
        fun getInstance(project: Project):ToolWindowTabsHelper{
            return project.getService(ToolWindowTabsHelper::class.java)
        }
    }

    fun isInsightsTab(content: Content?): Boolean {
        return content != null && content.tabName.equals(INSIGHTS_TAB_NAME, ignoreCase = true)
    }

    fun isErrorsTab(content: Content?): Boolean {
        return content != null && content.tabName.equals(ERRORS_TAB_NAME, ignoreCase = true)
    }

    fun isSummaryTab(content: Content?): Boolean {
        return content != null && content.tabName.equals(SUMMARY_TAB_NAME, ignoreCase = true)
    }


    fun showingErrorDetails() {
        visibleTabBeforeErrorDetails = toolWindow.contentManager.selectedContent
    }

    fun errorDetailsClosed() {
        visibleTabBeforeErrorDetails?.let {
            toolWindow.contentManager.setSelectedContent(it)
        }
        visibleTabBeforeErrorDetails = null
    }

    fun errorDetailsOn() {
        errorDetailsOn = true
        errorsContent.displayName = "Error Details"
    }

    fun errorDetailsOff() {
        errorDetailsOn = false
        errorsContent.displayName = "Errors"
    }

    fun isErrorDetailsOn(): Boolean {
        return errorDetailsOn
    }


}
