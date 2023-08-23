package org.digma.intellij.plugin.test.system

import com.intellij.openapi.Disposable
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


class MessageBusTestListeners(private val messageBus: MessageBus, private val parent: Disposable) {

    private val logger = Logger.getInstance(MessageBusTestListeners::class.java)

   

    fun registerSubToAnalyticsServiceConnectionEvent(onConnectionGained: () -> Unit, onConnectionLost: () -> Unit) {
        messageBus.connect(parent).subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            object : AnalyticsServiceConnectionEvent {
                override fun connectionGained() = onConnectionGained()
                override fun connectionLost() = onConnectionLost()
            })

    }

    fun registerSubToEnvironmentChangedEvent(onEnvChanged: (String?, Boolean) -> Unit) {
        messageBus.connect(parent).subscribe(
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
        messageBus.connect(parent).subscribe(
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
        messageBus.connect(parent).subscribe(
            ModelChangeListener.MODEL_CHANGED_TOPIC,
            ModelChangeListener {
                onModelChanged(it)
            })
    }
    
    fun disconnectAll() {
    }
}