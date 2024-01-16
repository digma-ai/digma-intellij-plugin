package org.digma.intellij.plugin.test.system.framework

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFile
import com.intellij.util.messages.MessageBus
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.document.DocumentInfoChanged
import org.digma.intellij.plugin.log.Log


//example class to test the message bus events
class MessageBusTestListeners(private val messageBus: MessageBus) {

    private val logger = Logger.getInstance(MessageBusTestListeners::class.java)


    /**
     * registers a subscriber to the AnalyticsServiceConnectionEvent
     * @param onConnectionGained - callback for when the connection is gained
     * @param onConnectionLost - callback for when the connection is lost
     */
    fun registerSubToAnalyticsServiceConnectionEvent(onConnectionGained: () -> Unit, onConnectionLost: () -> Unit) {
        messageBus.connect().subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            object : AnalyticsServiceConnectionEvent {
                override fun connectionGained() = onConnectionGained()
                override fun connectionLost() = onConnectionLost()
            })

    }


    /**
     * registers a subscriber to the EnvironmentChanged event
     * @param onEnvChanged - callback for when the environment is changed
     */
    fun registerSubToEnvironmentChangedEvent(onEnvChanged: (String?, Boolean) -> Unit) {
        messageBus.connect().subscribe(
            EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,
            object : EnvironmentChanged {
                override fun environmentChanged(newEnv: String?, refreshInsightsView: Boolean) {
                    onEnvChanged(newEnv, refreshInsightsView)
                }

                override fun environmentsListChanged(newEnvironments: MutableList<String>?) {
                    // Do nothing like some real listeners
                    Log.test(logger::info, "Test Subscriber - EnvironmentChanged: environmentsListChanged")
                }

            })
    }


    /**
     * registers a callback to DocumentInfoChanged event
     * @param onDocInfoChanged - callback for when the document info is changed
     */
    fun registerSubToDocumentInfoChangedEvent(onDocInfoChanged: (PsiFile) -> Unit) {
        messageBus.connect().subscribe(
            DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC,
            DocumentInfoChanged {
                onDocInfoChanged(it)
            })
    }
    
}