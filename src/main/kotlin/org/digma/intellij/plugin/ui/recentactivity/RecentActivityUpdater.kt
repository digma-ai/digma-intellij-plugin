package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.LOCAL_ENV
import org.digma.intellij.plugin.common.LOCAL_TESTS_ENV
import org.digma.intellij.plugin.common.SUFFIX_OF_LOCAL
import org.digma.intellij.plugin.common.SUFFIX_OF_LOCAL_TESTS
import org.digma.intellij.plugin.common.getSortedEnvironments
import org.digma.intellij.plugin.icons.AppIcons
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.recentactivity.RecentActivityLogic.Companion.isRecentTime
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivitiesMessagePayload
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivitiesMessageRequest
import java.util.Date
import java.util.Locale
import java.util.Optional
import javax.swing.Icon

private const val RECENT_ACTIVITY_SET_DATA = "RECENT_ACTIVITY/SET_DATA"

@Service(Service.Level.PROJECT)
class RecentActivityUpdater(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    private val recentActivityToolWindowIconChanger = RecentActivityToolWindowIconChanger(project)


    fun setJcefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }


    @Synchronized
    fun updateLatestActivities() {
        val environments = project.service<AnalyticsService>().environments
        environments?.let {
            updateLatestActivities(it)
        }

    }

    @Synchronized
    fun updateLatestActivities(environments: List<String>) {

        val latestActivityResult = project.service<RecentActivityService>().getRecentActivities(environments)

        latestActivityResult?.let {

            if (hasRecentActivity(latestActivityResult)) {
                recentActivityToolWindowIconChanger.showBadge()
            } else {
                recentActivityToolWindowIconChanger.hideBadge()
            }

            jCefComponent?.let {
                sendLatestActivities(it, latestActivityResult)
            }
        }
    }


    private fun hasRecentActivity(latestActivityResult: RecentActivityResult): Boolean {
        val latestActivity: Optional<Date> = latestActivityResult.entries.stream()
            .map(RecentActivityResponseEntry::latestTraceTimestamp)
            .max { obj: Date, anotherDate: Date? -> obj.compareTo(anotherDate) }

        return latestActivity.isPresent && isRecentTime(latestActivity.get())
    }


    private fun sendLatestActivities(jCefComponent: JCefComponent, latestActivityResult: RecentActivityResult) {

        Log.log(logger::debug, "updating recent activities")

        val allEnvironments = project.service<AnalyticsService>().environment.getEnvironments()

        val sortedEnvironments = getSortedEnvironments(allEnvironments, CommonUtils.getLocalHostname())


        Log.log(logger::trace, "updating recent activities with result {}", latestActivityResult)

        val recentActivitiesMessage = RecentActivitiesMessageRequest(
            JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
            RECENT_ACTIVITY_SET_DATA,
            RecentActivitiesMessagePayload(
                sortedEnvironments,
                getEntriesWithAdjustedLocalEnvs(latestActivityResult)
            )
        )

        serializeAndExecuteWindowPostMessageJavaScript(jCefComponent.jbCefBrowser.cefBrowser, recentActivitiesMessage)
    }


    private fun getEntriesWithAdjustedLocalEnvs(recentActivityData: RecentActivityResult): List<RecentActivityResponseEntry> {
        return recentActivityData.entries.stream()
            .map { (environment, traceFlowDisplayName, firstEntrySpan, lastEntrySpan, latestTraceId, latestTraceTimestamp, latestTraceDuration, slimAggregatedInsights): RecentActivityResponseEntry ->
                RecentActivityResponseEntry(
                    getAdjustedEnvName(environment),
                    traceFlowDisplayName,
                    firstEntrySpan,
                    lastEntrySpan,
                    latestTraceId,
                    latestTraceTimestamp,
                    latestTraceDuration,
                    slimAggregatedInsights
                )
            }.toList()
    }

    private fun getAdjustedEnvName(environment: String): String {
        val envUppercase = environment.uppercase(Locale.getDefault())
        if (envUppercase.endsWith(SUFFIX_OF_LOCAL)) return LOCAL_ENV
        return if (envUppercase.endsWith(SUFFIX_OF_LOCAL_TESTS)) LOCAL_TESTS_ENV else environment
    }

    override fun dispose() {
        //nothing to do , used as parent disposable
    }
}


private class RecentActivityToolWindowIconChanger(val project: Project) {

    private val defaultIcon = AppIcons.TOOL_WINDOW_OBSERVABILITY
    private var actualIcon: Icon? = null
    private var badgeIcon: Icon? = null


    fun showBadge() {

        val toolWindow = getToolWindow() ?: return

        createBadgeIcon()

        EDT.ensureEDT {
            badgeIcon?.let {
                toolWindow.setIcon(it)
            }
        }
    }

    private fun createBadgeIcon() {

        if (badgeIcon == null) {
            val icon = actualIcon ?: defaultIcon
            badgeIcon = ExecutionUtil.getLiveIndicator(icon)
        }
    }


    fun hideBadge() {

        //if badgeIcon is null then it was never created and no need to do anything
        if (badgeIcon == null) {
            return
        }

        val toolWindow = getToolWindow() ?: return

        EDT.ensureEDT {
            toolWindow.setIcon(actualIcon ?: defaultIcon)
        }
    }

    private fun getToolWindow(): ToolWindow? {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PluginId.OBSERVABILITY_WINDOW_ID)

        if (actualIcon == null) {
            //capture the actual icon first time we got a non-null tool window.
            // and make sure it is initialized at least to default icon
            actualIcon = toolWindow?.icon ?: defaultIcon
        }

        return toolWindow
    }

}