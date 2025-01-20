package org.digma.intellij.plugin.ui.notificationcenter

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.updates.AggressiveUpdateStateChangedEvent
import org.digma.intellij.plugin.updates.CurrentUpdateState
import org.digma.intellij.plugin.updates.PublicUpdateState

const val STICKY_REMINDERS_NOTIFICATION_GROUP = "Digma sticky Reminders Group"
const val FADING_REMINDERS_NOTIFICATION_GROUP = "Digma fading Reminders Group"


@Service(Service.Level.APP)
class AppNotificationCenter : Disposable {

    //need to keep them in order to expire them when exiting update mode
    private val currentlyShowingAggressiveUpdateNotifications = mutableListOf<Notification>()

    private var aggressiveUpdateTimerDisposable: Disposable? = null

    companion object {
        val logger = Logger.getInstance(this::class.java)

        fun getInstance(): AppNotificationCenter {
            return service<AppNotificationCenter>()
        }
    }


    override fun dispose() {
        //nothing to do , used as parent disposable
    }

    init {
        Log.log(logger::info, "Starting notification center")
        startNoInsightsYetNotificationTimer()

        startUsingTheCliNotificationTimer()

        startIdleUserTimers(this)

        startRequestRegisterTimers(this)

    }


    //this listener is registered in plugin.xml, so it will receive the event early if an event will be fired.
    class AggressiveUpdateStateChangedEventListener(val project: Project) : AggressiveUpdateStateChangedEvent {
        override fun stateChanged(updateState: PublicUpdateState) {
            //cancel previous task by disposing the aggressiveUpdateTimerDisposable.
            getInstance().aggressiveUpdateTimerDisposable?.let {
                Disposer.dispose(it)
            }

            if (updateState.updateState == CurrentUpdateState.OK) {
                getInstance().clearCurrentlyShowingAggressiveUpdateNotifications()
            } else {
                getInstance().aggressiveUpdateTimerDisposable = Disposer.newDisposable()
                getInstance().aggressiveUpdateTimerDisposable?.let {
                    //also register as child of this service, so it is disposed with service
                    Disposer.register(getInstance(), it)
                    startAggressiveUpdateNotificationTimer(
                        it,
                        project,
                        getInstance().currentlyShowingAggressiveUpdateNotifications
                    )
                }
            }
        }
    }


    fun clearCurrentlyShowingAggressiveUpdateNotifications() {
        currentlyShowingAggressiveUpdateNotifications.forEach { it.expire() }
        currentlyShowingAggressiveUpdateNotifications.clear()
    }


    private fun startNoInsightsYetNotificationTimer() {

        if (service<PersistenceService>().isNoInsightsYetNotificationPassed()) {
            Log.log(logger::info, "noInsightsYetNotificationPassed already passed")
            return
        }

        Log.log(logger::info, "maybe starting NoInsightsReminderNotificationTimer")
        startNoInsightsYetNotificationTimer(this)

    }

    private fun startUsingTheCliNotificationTimer() {

        if (service<PersistenceService>().isUsingTheCliNotificationPassed()) {
            Log.log(logger::info, "UsingTheCliNotificationPassed already passed")
            return
        }

        Log.log(logger::info, "maybe starting UsingTheCliNotificationTimer")
        startUsingTheCliNotificationTimer(this)

    }


    fun showInstallationInProgressNotification(project: Project) {
        Log.log(logger::info, "showing InstallationInProgressNotification")
        NotificationGroupManager.getInstance().getNotificationGroup(FADING_REMINDERS_NOTIFICATION_GROUP)
            .createNotification("Digma installation is still running", NotificationType.INFORMATION)
            .notify(project)
    }


    fun showInstallationFinishedNotification(project: Project) {

        if (ToolWindowShower.getInstance(project).isToolWindowVisible) {
            return
        }

        Log.log(logger::info, "showing InstallationFinishedNotification")

        ActivityMonitor.getInstance(project).registerNotificationCenterEvent("Show.InstallationFinishedNotification", mapOf())

        val notification = NotificationGroupManager.getInstance().getNotificationGroup(STICKY_REMINDERS_NOTIFICATION_GROUP)
            .createNotification(
                "Digma successfully installed",
                "Please follow the onboarding steps to run your application with Digma",
                NotificationType.INFORMATION
            )

        notification.addAction(ShowToolWindowAction(project, notification, "InstallationFinishedNotification"))

        notification.notify(project)
    }
}