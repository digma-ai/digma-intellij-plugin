package org.digma.intellij.plugin.test.system.framework

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFile
import com.intellij.util.messages.MessageBus
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.analytics.TabsChanged
import org.digma.intellij.plugin.document.DocumentInfoChanged
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.ModelChangeListener
import org.digma.intellij.plugin.ui.model.PanelModel


class MessageBusTestListeners(val messageBus: MessageBus) {

    private val logger = Logger.getInstance(MessageBusTestListeners::class.java)

   

    fun registerSubToAnalyticsServiceConnectionEvent(onConnectionGained: () -> Unit, onConnectionLost: () -> Unit) {
        messageBus.connect().subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            object : AnalyticsServiceConnectionEvent {
                override fun connectionLost() {
                    onConnectionGained()

                }

                override fun connectionGained() {
                    onConnectionLost()

                }

            })

    }

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

    fun registerSubToTabsChangedEvent(onTabsChanged: (Int) -> Unit) {
        messageBus.connect().subscribe(
            TabsChanged.TABS_CHANGED_TOPIC,
            TabsChanged {
                onTabsChanged(it)
               
            })
    }

    fun registerSubToDocumentInfoChangedEvent(onDocInfoChanged: (PsiFile) -> Unit) {
        messageBus.connect().subscribe(
            DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC,
            DocumentInfoChanged {
                onDocInfoChanged(it)
            })
    }

    fun registerSubToModelChangedEvent(onModelChanged: (PanelModel) -> Unit) {
        messageBus.connect().subscribe(
            ModelChangeListener.MODEL_CHANGED_TOPIC,
            ModelChangeListener {
                onModelChanged(it)
            })
    }
}