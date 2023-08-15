package org.digma.intellij.plugin.test.system

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import junit.framework.TestCase
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.analytics.TabsChanged
import org.digma.intellij.plugin.document.DocumentInfoChanged
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.ModelChangeListener
import org.digma.intellij.plugin.test.system.framework.WaitFinishRule
import org.digma.intellij.plugin.ui.model.PanelModel


class MessageBusTestListeners(private val project:Project, val waitRule: WaitFinishRule) {
    
    private val logger = Logger.getInstance(MessageBusTestListeners::class.java)
    
    fun registerToAllEventsForLogging() {
        registerSubToAnalyticsServiceConnectionEvent({
            Log.test(logger::info, "Test Subscriber - AnalyticsServiceConnectionEvent: connectionGained")
            TestCase.fail("this should happen only once")
        }, {
            Log.test(logger::info, "Test Subscriber - AnalyticsServiceConnectionEvent: connectionLost")
        })
        registerSubToEnvironmentChangedEvent { _, _ -> 
            Log.test(logger::info, "Test Subscriber - EnvironmentChanged: environmentChanged")
        }
        registerSubToTabsChangedEvent { 
            Log.test(logger::info, "Test Subscriber - TabsChanged: tabsChanged")
        }
        registerSubToDocumentInfoChangedEvent { 
            Log.test(logger::info, "Test Subscriber - DocumentInfoChanged: documentInfoChanged")
            TestCase.fail("This is intended to be called, remove the line to properly test")
        }
        registerSubToModelChangedEvent { 
            Log.test(logger::info, "Test Subscriber - ModelChangeListener: modelChanged")
        }  
    }

    fun registerSubToAnalyticsServiceConnectionEvent(onConnectionGained: ()-> Unit, onConnectionLost: ()-> Unit) {
        project.messageBus.connect().subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC,
            object: AnalyticsServiceConnectionEvent {
                override fun connectionLost() {
                    onConnectionGained()
                    waitRule.signalComplete()
                }

                override fun connectionGained() {
                    onConnectionLost()
                    waitRule.signalComplete()
                }

            })

    }

    fun registerSubToEnvironmentChangedEvent(onEnvHanged: (String?, Boolean) -> Unit) {
        project.messageBus.connect().subscribe(
            EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,
            object : EnvironmentChanged {
                override fun environmentChanged(newEnv: String?, refreshInsightsView: Boolean) {
                    onEnvHanged(newEnv, refreshInsightsView)
                    waitRule.signalComplete()
                }

                override fun environmentsListChanged(newEnvironments: MutableList<String>?) {
                    // Do nothing like some real listeners
                    Log.test(logger::info, "Test Subscriber - EnvironmentChanged: environmentsListChanged")
                }

            })
    }

    fun registerSubToTabsChangedEvent(onTabsChanged: (Int) -> Unit) {
        project.messageBus.connect().subscribe(
            TabsChanged.TABS_CHANGED_TOPIC,
            TabsChanged {
                onTabsChanged(it)
                waitRule.signalComplete()
            })
    }

    fun registerSubToDocumentInfoChangedEvent(onDocInfoChanged: (PsiFile) -> Unit) {
        project.messageBus.connect().subscribe(
            DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC,
            DocumentInfoChanged {
                onDocInfoChanged(it)
                waitRule.signalComplete()
            })
    }

    fun registerSubToModelChangedEvent(onModelChanged: (PanelModel) -> Unit) {
        project.messageBus.connect().subscribe(
            ModelChangeListener.MODEL_CHANGED_TOPIC,
            ModelChangeListener {
                onModelChanged(it)
                waitRule.signalComplete()
            })
    }
}