package org.digma.intellij.plugin.ui.notificationcenter

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.digma.intellij.plugin.log.Log

class NotificationsStartupActivity: StartupActivity {
    override fun runActivity(project: Project) {
        Log.log(AppNotificationCenter.logger::info,"NotificationsStartupActivity called")
        project.service<EventsNotificationsService>().waitForEvents()
        service<AppNotificationCenter>() //just make sure to start the service
    }
}