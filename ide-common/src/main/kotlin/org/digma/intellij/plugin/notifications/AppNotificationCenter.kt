package org.digma.intellij.plugin.notifications

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.ToolWindowShower
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.APP)
class AppNotificationCenter: Disposable {

    companion object{
        val logger = Logger.getInstance(this::class.java)
    }


    private val noInsightsYetNotificationTimerStarted: AtomicBoolean = AtomicBoolean(false)

    override fun dispose() {
        //nothing to do , used as parent disposable
    }

    init {
        Log.log(logger::info,"Starting notification center")
        startNoInsightsYetNotificationTimer()
    }

    private fun startNoInsightsYetNotificationTimer() {

        if (service<PersistenceService>().state.noInsightsYetNotificationPassed){
            Log.log(logger::info,"noInsightsYetNotificationPassed already passed")
            return
        }

        if (noInsightsYetNotificationTimerStarted.get()){
            return
        }

        noInsightsYetNotificationTimerStarted.set(true)

        Log.log(logger::info,"maybe starting NoInsightsReminderNotificationTimer")
        startNoInsightsYetNotificationTimer(this)
    }



    fun showInstallationInProgressNotification(project: Project) {
        Log.log(logger::info,"showing InstallationInProgressNotification")
        NotificationGroupManager.getInstance().getNotificationGroup(FADING_REMINDERS_NOTIFICATION_GROUP)
            .createNotification( "Digma installation is still running", NotificationType.INFORMATION)
            .notify(project)
    }


    fun showInstallationFinishedNotification(project: Project) {

        if (ToolWindowShower.getInstance(project).isToolWindowVisible){
            return
        }

        Log.log(logger::info,"showing InstallationFinishedNotification")

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.InstallationFinishedNotification",mapOf())

        val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
            .createNotification( "Digma successfully installed","Please follow the onboarding steps to run your application with Digma", NotificationType.INFORMATION)

        notification.addAction(ShowToolWindowAction(project,notification,"InstallationFinishedNotification"))

        notification.notify(project)
    }
}