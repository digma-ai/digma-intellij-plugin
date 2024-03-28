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
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.analytics.getAllEnvironments
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.icons.AppIcons
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.environment.Env
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivitiesMessagePayload
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivitiesMessageRequest
import org.digma.intellij.plugin.ui.recentactivity.model.RecentActivityEnvironment
import java.time.Instant
import java.time.temporal.ChronoUnit
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
            override fun environmentChanged(newEnv: Env) {
                Backgroundable.ensurePooledThread { updateLatestActivities() }
            }

            override fun environmentsListChanged(newEnvironments: List<Env>) {
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
        val environments = getAllEnvironments(project)
        if (environments.isEmpty()) {
            recentActivityToolWindowIconChanger.hideBadge()
            sendEmptyData()
        } else {
            Log.log(logger::trace, "got environments from backend {}", environments)
            updateLatestActivities(environments)
        }

    }

    @Synchronized
    fun updateLatestActivities(environments: List<Env>) {

        Log.log(logger::trace, "updateLatestActivities(List<String>) called")

        val latestActivitiesResult =
            project.service<RecentActivityService>().getRecentActivities(environments.map { env: Env -> env.id })

        Log.log(logger::trace, "got latestActivitiesResult {}", latestActivitiesResult)

        if (latestActivitiesResult != null) {

            if (hasRecentActivity(latestActivitiesResult)) {
                Log.log(logger::trace, "hasRecentActivity recentActivities,changing tool window to badged icon")
                recentActivityToolWindowIconChanger.showBadge()
            } else {
                Log.log(logger::trace, "not hasRecentActivity recentActivities,changing tool window to regular icon")
                recentActivityToolWindowIconChanger.hideBadge()
            }

            jCefComponent?.let {
                sendLatestActivities(it, latestActivitiesResult, environments)
            }
        } else {
            recentActivityToolWindowIconChanger.hideBadge()
            sendEmptyData()
        }
    }


    private fun hasRecentActivity(latestActivityResult: RecentActivityResult): Boolean {
        val latestActivity: Optional<Date> = latestActivityResult.entries.stream()
            .map(RecentActivityResponseEntry::latestTraceTimestamp)
            .max { obj: Date, anotherDate: Date? -> obj.compareTo(anotherDate) }

        return latestActivity.isPresent && isRecentTime(latestActivity.get())
    }


    private fun isRecentTime(date: Date?): Boolean {
        if (date == null) return false
        return date.toInstant().plus(RECENT_EXPIRATION_LIMIT_MILLIS, ChronoUnit.MILLIS).isAfter(Instant.now())
    }


    private fun sendLatestActivities(jCefComponent: JCefComponent, latestActivitiesResult: RecentActivityResult, environments: List<Env>) {

        Log.log(logger::trace, "updating recent activities {},environments:{}", latestActivitiesResult, environments)

        Log.log(logger::trace, "got environments {}", environments)

        val recentActivityEnvironments = environments.map {
            RecentActivityEnvironment(
                it.id,
                it.name,
                it.type,
                null,
                null,
                null,
                false
            )
        }

        Log.log(logger::trace, "updating recent activities with result {},{}", latestActivitiesResult, recentActivityEnvironments)

        val recentActivitiesMessage = RecentActivitiesMessageRequest(
            JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
            RECENT_ACTIVITY_SET_DATA,
            RecentActivitiesMessagePayload(
                recentActivityEnvironments,
                latestActivitiesResult.entries
            )
        )

        Log.log(logger::trace, "sending recentActivitiesMessage to app {}", recentActivitiesMessage)
        serializeAndExecuteWindowPostMessageJavaScript(jCefComponent.jbCefBrowser.cefBrowser, recentActivitiesMessage)
    }



    private fun sendEmptyData() {

        jCefComponent?.let { jcef ->

            val recentActivitiesMessage = RecentActivitiesMessageRequest(
                JCEFGlobalConstants.REQUEST_MESSAGE_TYPE,
                RECENT_ACTIVITY_SET_DATA,
                RecentActivitiesMessagePayload(
                    listOf(),
                    listOf()
                )
            )

            Log.log(logger::trace, "sending empty recentActivitiesMessage to app {}", recentActivitiesMessage)
            serializeAndExecuteWindowPostMessageJavaScript(jcef.jbCefBrowser.cefBrowser, recentActivitiesMessage)
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