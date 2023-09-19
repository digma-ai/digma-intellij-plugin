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
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.LOCAL_ENV
import org.digma.intellij.plugin.common.LOCAL_TESTS_ENV
import org.digma.intellij.plugin.common.isEnvironmentLocal
import org.digma.intellij.plugin.common.isEnvironmentLocalTests
import org.digma.intellij.plugin.common.isLocalEnvironmentMine
import org.digma.intellij.plugin.icons.AppIcons
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.recentactivity.RecentActivityLogic.Companion.isRecentTime
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.recentactivity.model.EnvironmentType
import org.digma.intellij.plugin.ui.recentactivity.model.PendingEnvironment
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivitiesMessagePayload
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivitiesMessageRequest
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityEnvironment
import java.util.Date
import java.util.Optional
import javax.swing.Icon

private const val RECENT_ACTIVITY_SET_DATA = "RECENT_ACTIVITY/SET_DATA"

@Service(Service.Level.PROJECT)
class RecentActivityUpdater(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    private val recentActivityToolWindowIconChanger = RecentActivityToolWindowIconChanger(project)


    init {
        project.messageBus.connect(this).subscribe<EnvironmentChanged>(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, object : EnvironmentChanged {
            override fun environmentChanged(newEnv: String?, refreshInsightsView: Boolean) {
                Backgroundable.ensurePooledThread { updateLatestActivities() }
            }

            override fun environmentsListChanged(newEnvironments: List<String>) {
                //nothing to do
            }
        })

    }


    fun setJcefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }


    @Synchronized
    fun updateLatestActivities() {
        Log.log(logger::trace, "updateLatestActivities called")
        val environments = project.service<AnalyticsService>().environments
        environments?.let { envs ->

            Log.log(logger::trace, "got environments from backend {}", envs)

            updateLatestActivities(envs)
        }

    }

    @Synchronized
    fun updateLatestActivities(environments: List<String>) {

        Log.log(logger::trace, "updateLatestActivities(List<String>) called")

        val latestActivitiesResult = project.service<RecentActivityService>().getRecentActivities(environments)

        Log.log(logger::trace, "got latestActivitiesResult {}", latestActivitiesResult)

        latestActivitiesResult?.let { recentActivities ->

            if (hasRecentActivity(recentActivities)) {
                Log.log(logger::trace, "hasRecentActivity recentActivities,changing tool window to badged icon")
                recentActivityToolWindowIconChanger.showBadge()
            } else {
                Log.log(logger::trace, "not hasRecentActivity recentActivities,changing tool window to regular icon")
                recentActivityToolWindowIconChanger.hideBadge()
            }

            jCefComponent?.let {
                sendLatestActivities(it, recentActivities, environments)
            }
        }
    }


    private fun hasRecentActivity(latestActivityResult: RecentActivityResult): Boolean {
        val latestActivity: Optional<Date> = latestActivityResult.entries.stream()
            .map(RecentActivityResponseEntry::latestTraceTimestamp)
            .max { obj: Date, anotherDate: Date? -> obj.compareTo(anotherDate) }

        return latestActivity.isPresent && isRecentTime(latestActivity.get())
    }


    private fun sendLatestActivities(jCefComponent: JCefComponent, latestActivitiesResult: RecentActivityResult, environments: List<String>) {

        Log.log(logger::trace, "updating recent activities {},environments:{}", latestActivitiesResult, environments)

        removeFromPendingEnvironments(environments)

//        val sortedEnvironments = getSortedEnvironments(environments, CommonUtils.getLocalHostname())

        Log.log(logger::trace, "got environments {}", environments)

        val pendingEnvironments = service<AddEnvironmentsService>().getPendingEnvironments()

        Log.log(logger::trace, "got pendingEnvironments {}", pendingEnvironments)

        val allEnvs = mergeWithPendingEnvironments(environments, pendingEnvironments)

        Log.log(logger::trace, "got allEnvs {}", allEnvs)

        Log.log(logger::trace, "updating recent activities with result {},{}", latestActivitiesResult, allEnvs)

        val recentActivitiesMessage = RecentActivitiesMessageRequest(
            JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
            RECENT_ACTIVITY_SET_DATA,
            RecentActivitiesMessagePayload(
                allEnvs,
                getEntriesWithAdjustedLocalEnvs(latestActivitiesResult)
            )
        )

        Log.log(logger::trace, "sending recentActivitiesMessage to app {}", recentActivitiesMessage)
        serializeAndExecuteWindowPostMessageJavaScript(jCefComponent.jbCefBrowser.cefBrowser, recentActivitiesMessage)
    }

    private fun mergeWithPendingEnvironments(
        environments: List<String>,
        pendingEnvironments: List<PendingEnvironment>,
    ): List<RecentActivityEnvironment> {

        val allEnvs = mutableListOf<RecentActivityEnvironment>()

        val recentActivityEnvs = buildRecentActivityEnvs(environments)
        allEnvs.addAll(recentActivityEnvs)

        val pendingEnvs = pendingEnvironments.map { p: PendingEnvironment ->
            RecentActivityEnvironment(
                p.name,
                p.name,
                true,
                p.additionToConfigResult,
                p.type
            )
        }
        allEnvs.addAll(pendingEnvs)

        allEnvs.sortBy { recentActivityEnvironment: RecentActivityEnvironment -> recentActivityEnvironment.name }

        return allEnvs
    }

    private fun buildRecentActivityEnvs(environments: List<String>): List<RecentActivityEnvironment> {

        val hostname = CommonUtils.getLocalHostname()

        val transformEnvToRecentActivityEnvironment: (String) -> RecentActivityEnvironment = { env ->

            val displayName = if (isEnvironmentLocal(env) && isLocalEnvironmentMine(env, hostname)) {
                LOCAL_ENV
            } else if (isEnvironmentLocalTests(env) && isLocalEnvironmentMine(env, hostname)) {
                LOCAL_TESTS_ENV
            } else {
                env
            }

            val type: EnvironmentType =
                if (displayName == LOCAL_ENV || displayName == LOCAL_TESTS_ENV) EnvironmentType.local else EnvironmentType.shared

            RecentActivityEnvironment(displayName, env, false, null, type, null, null)
        }

        return environments.map(transformEnvToRecentActivityEnvironment)
    }


    private fun getEntriesWithAdjustedLocalEnvs(recentActivityData: RecentActivityResult): List<RecentActivityResponseEntry> {
        return recentActivityData.entries.stream()
            .map { (environment, traceFlowDisplayName, firstEntrySpan, lastEntrySpan, latestTraceId, latestTraceTimestamp, latestTraceDuration, slimAggregatedInsights): RecentActivityResponseEntry ->
                RecentActivityResponseEntry(
                    environment,
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

//    private fun getAdjustedEnvName(environment: String): String {
//        val envUppercase = environment.uppercase(Locale.getDefault())
//        if (envUppercase.endsWith(SUFFIX_OF_LOCAL)) return LOCAL_ENV
//        return if (envUppercase.endsWith(SUFFIX_OF_LOCAL_TESTS)) LOCAL_TESTS_ENV else environment
//    }


    private fun removeFromPendingEnvironments(environments: List<String>) {
        environments.forEach { env ->
            if (service<AddEnvironmentsService>().isPendingEnv(env)) {
                Log.log(logger::info, "found environment {} from backend in pending environments, removing it from pending", env)
                service<AddEnvironmentsService>().removeEnvironment(env)
            }
        }
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