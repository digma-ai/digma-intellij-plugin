package org.digma.intellij.plugin.analytics

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.Alarm
import com.intellij.util.Alarm.ThreadToUse
import org.digma.intellij.plugin.common.DisposableAdaptor

@Service(Service.Level.APP)
class ProjectToAppBackendEventsBridge : DisposableAdaptor {

    /*
    the motivation of this bridge:
    AnalyticsService is a project level service, when connection lost/gained it will fire an event
    for each project and project level listeners to AnalyticsServiceConnectionEvent will react.
    the same for ApiClientChangedEvent.
    we also have some application level services that are interested in connection lost/gained and
    in api client change, those services don't have a project message-bus to subscribe, and even if they did
    it is unnecessary for them to react on events from all open projects.
    these services listen to app level topics.
    this bridge will catch the project level events and fire a corresponding app level event. it has a quite period
    so that it will not fire several events at a time.
    for example: if we have 3 projects open, and there is a connection lost, each of these projects will fire an
    event, they will probably all fire it within the same time period. this bridge will catch all these events
    and try to minimize the application events to one, or at least not as many as open projects.
    */

    private val connectionEventAlarm = Alarm(ThreadToUse.POOLED_THREAD, this)
    private val apiClientChangeAlarm = Alarm(ThreadToUse.POOLED_THREAD, this)


    fun registerListeners(project: Project) {

        project.messageBus.connect()
            .subscribe(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
                object : AnalyticsServiceConnectionEvent {
                    override fun connectionLost() {

                        connectionEventAlarm.cancelAllRequests()
                        connectionEventAlarm.addRequest({
                            ApplicationManager.getApplication().messageBus
                                .syncPublisher(AnalyticsServiceAppLevelConnectionEvent.ANALYTICS_SERVICE_APP_LEVEL_CONNECTION_EVENT_TOPIC)
                                .connectionLost()
                        }, 2000)
                    }

                    override fun connectionGained() {

                        connectionEventAlarm.cancelAllRequests()
                        connectionEventAlarm.addRequest({
                            ApplicationManager.getApplication().messageBus
                                .syncPublisher(AnalyticsServiceAppLevelConnectionEvent.ANALYTICS_SERVICE_APP_LEVEL_CONNECTION_EVENT_TOPIC)
                                .connectionGained()
                        }, 2000)
                    }
                })


        project.messageBus.connect().subscribe(ApiClientChangedEvent.API_CLIENT_CHANGED_TOPIC,
            ApiClientChangedEvent { url ->

                apiClientChangeAlarm.cancelAllRequests()
                apiClientChangeAlarm.addRequest({
                    ApplicationManager.getApplication().messageBus.syncPublisher(ApiClientChangedAppLevelEvent.API_CLIENT_CHANGED_APP_LEVEL_TOPIC)
                        .apiClientChanged(url)
                }, 2000)
            })
    }


}