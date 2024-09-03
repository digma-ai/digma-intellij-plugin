package org.digma.intellij.plugin.ui.notificationcenter

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.startup.DigmaProjectActivity

class NotificationsStartupActivity: DigmaProjectActivity() {
    override fun executeProjectStartup(project: Project) {
        Log.log(AppNotificationCenter.logger::info,"NotificationsStartupActivity called")

        //we disabled EventsNotificationsService because FirstImportantInsightEvent is duplicate of
        //notifications shown by UserActivationService
        ////project.service<EventsNotificationsService>().waitForEvents()
        service<AppNotificationCenter>() //just make sure to start the service
    }
}