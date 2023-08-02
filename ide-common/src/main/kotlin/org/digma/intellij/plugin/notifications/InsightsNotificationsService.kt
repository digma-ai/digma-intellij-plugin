package org.digma.intellij.plugin.notifications

import com.intellij.collaboration.async.DisposingScope
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService


const val MY_NOTIFICATION_GROUP = "Digma Events Group"

@Service(Service.Level.PROJECT)
class InsightsNotificationsService(val project: Project):Disposable {

    private val logger = Logger.getInstance(this::class.java)

    fun waitForFirstImportantInsight(){

        Log.log(logger::info,"starting insights notification service")

        if(PersistenceService.getInstance().state.isFirstImportantInsightReceived){
            return
        }


        DisposingScope(this).launch {

            while (!PersistenceService.getInstance().state.isFirstImportantInsightReceived) {

//                val events = project.service<AnalyticsService>().getLatestEvents()

                delay(30000)
                showInsightNotification("my name","my\$_\$codeobject")
                PersistenceService.getInstance().state.isFirstImportantInsightReceived = true

            }

        }




    }

    private fun showInsightNotification(insightName: String,codeObjectId:String) {

        val notifivcation = NotificationGroupManager.getInstance().getNotificationGroup(MY_NOTIFICATION_GROUP)
            .createNotification("Digma found an important insight about your code:",insightName,NotificationType.INFORMATION)
            .addAction(GoToCoeObjectInsightsAction(codeObjectId))


            .notify(project)

    }

    override fun dispose() {
        //nothing to do
    }


}


class GoToCoeObjectInsightsAction(val codeObjectId:String): AnAction("Show Insights") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        project?.service<InsightsViewOrchestrator>()?.showInsightsForSpanOrMethodAndNavigateToCode(codeObjectId,codeObjectId)
    }

}