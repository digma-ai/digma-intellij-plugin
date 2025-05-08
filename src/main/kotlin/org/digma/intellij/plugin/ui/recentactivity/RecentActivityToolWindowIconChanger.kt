package org.digma.intellij.plugin.ui.recentactivity

import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.analytics.getAllEnvironments
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.icons.AppIcons
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import java.util.Date
import javax.swing.Icon
import kotlin.time.Duration.Companion.seconds

//must be a singleton because it relies on null and non-null properties
@Service(Service.Level.PROJECT)
class RecentActivityToolWindowIconChanger(val project: Project): DisposableAdaptor {

    private val logger = Logger.getInstance(this::class.java)

    private val defaultIcon = AppIcons.TOOL_WINDOW_OBSERVABILITY
    private var actualIcon: Icon? = null
    private var badgeIcon: Icon? = null


    companion object{
        fun getInstance(project: Project): RecentActivityToolWindowIconChanger {
            return project.service<RecentActivityToolWindowIconChanger>()
        }
    }

    init {
        val registered = disposingPeriodicTask("RecentActivityIconChanger",10.seconds.inWholeMilliseconds,10.seconds.inWholeMilliseconds,true){
            val environments = getAllEnvironments(project)
            val lastActiveTime: Date? = environments
                .filter { it.lastActive != null }
                .maxByOrNull { it.lastActive!! }
                ?.lastActive

            val isRecent = lastActiveTime?.let {
                val now = System.currentTimeMillis()
                now - it.time < RECENT_EXPIRATION_LIMIT_MILLIS
            } ?: false

            if (isRecent){
                showBadge()
            }else{
                hideBadge()
            }
        }
        if (!registered){
            Log.log(logger::warn,"could not register recent activity icon changer scheduler")
            ErrorReporter.getInstance().reportError(project,"RecentActivityToolWindowIconChanger.scheduler","could not register recent activity icon changer scheduler",mapOf())
        }
    }


    fun showBadge() {

        val toolWindow = getToolWindow() ?: return

        createBadgeIcon()

        EDT.ensureEDT {
            if (toolWindow.icon != badgeIcon) {
                badgeIcon?.let {
                    toolWindow.setIcon(it)
                }
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
            val icon = actualIcon ?: defaultIcon
            if (toolWindow.icon != icon) {
                toolWindow.setIcon(icon)
            }
        }
    }

    private fun getToolWindow(): ToolWindow? {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PluginId.OBSERVABILITY_WINDOW_ID)

        if (actualIcon == null) {
            //capture the actual icon the first time we got a non-null tool window.
            // and make sure it is initialized at least to the default icon
            actualIcon = toolWindow?.icon ?: defaultIcon
        }

        return toolWindow
    }

}