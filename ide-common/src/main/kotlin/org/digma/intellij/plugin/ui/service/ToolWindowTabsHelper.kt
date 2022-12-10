package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import kotlin.math.abs

class ToolWindowTabsHelper(val project: Project) {

    lateinit var toolWindow: ToolWindow

    lateinit var insightsContent: Content
    lateinit var errorsContent: Content

    var visibleTabBeforeErrorDetails: Content? = null
    var errorDetailsOn = false

    companion object {
        const val INSIGHTS_TAB_NAME = "insights"
        const val ERRORS_TAB_NAME = "errors"
        const val SUMMARY_TAB_NAME = "summary"
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

    fun showInsightsTab() {
        toolWindow.contentManager.setSelectedContent(insightsContent)
    }

}

fun needToShowDurationChange(percentile: SpanDurationsPercentile): Boolean {
    val tolerationConstant: Long = 10000

    if (percentile.previousDuration != null && percentile.changeTime != null) {
        val rawDiff: Long = abs(percentile.currentDuration.raw - percentile.previousDuration!!.raw)
        return ((rawDiff.toFloat() / percentile.previousDuration!!.raw) > 0.1) && (rawDiff > tolerationConstant)
    }
    return false
}