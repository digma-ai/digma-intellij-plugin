package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.navigation.InsightsAndErrorsTabsHelper
import org.digma.intellij.plugin.ui.model.NOT_SUPPORTED_OBJECT_MSG
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel


abstract class AbstractViewService(val project: Project) : Disposable {

    //these may be null if the tool window did not open yet
    var panel: DigmaTabPanel? = null

    private val analyticsConnectionEventsConnection: MessageBusConnection = project.messageBus.connect()


    init {
        //subscribe to connection lost/gained , call doUpdateUi() on each event so that the no connection card will show or hide
        analyticsConnectionEventsConnection.subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            handler = object : AnalyticsServiceConnectionEvent {
                override fun connectionLost() {
                    doConnectionLost()
                    doUpdateUi()
                }

                override fun connectionGained() {
                    doConnectionGained()
                    doUpdateUi()
                    }
                }
            )
    }


    fun doConnectionLost() {
        val insightsAndErrorsTabsHelper = project.service<InsightsAndErrorsTabsHelper>()
        //if a view needs to do something when connection lost can override this method and don't forget to call super
        if (insightsAndErrorsTabsHelper.isErrorDetailsOn()) {
            insightsAndErrorsTabsHelper.errorDetailsOff()
            if (this is ErrorsViewService) {
                this.closeErrorDetails()
            }
            insightsAndErrorsTabsHelper.errorDetailsClosed()
        }
    }

    fun doConnectionGained() {
        //if a view needs to do something when connection gained can override this method and don't forget to call super
    }

    abstract fun getViewDisplayName(): String

    //in some situation the UI should not be updated, for example if the error details is On then nothing changes
    //in the view until its closed.
    open fun canUpdateUI(): Boolean {
        return true
    }



    open fun updateUi() {

        //don't update ui if error details is on,
        //when error details is closed the ui will be updated.
        //the models do get updated on every context changed

        if (!canUpdateUI()) {
            return
        }

        doUpdateUi()
    }


    //this method should never be called directly,only when connection is lost or gained.
    //it by-pass the conditions usually tested in updateUi so that the no-connection card will show on all tabs.
    private fun doUpdateUi() {

        if (panel != null) {

            val r = Runnable {
                panel?.reset()
            }

            EDT.ensureEDT(r)
        }
    }

    protected fun getNonSupportedFileScopeMessage(fileUri: String?): String {
        return NOT_SUPPORTED_OBJECT_MSG + " " + fileUri?.substringAfterLast('/', fileUri)
    }


    override fun dispose() {
        analyticsConnectionEventsConnection.dispose()
    }
}