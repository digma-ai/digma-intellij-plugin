package org.digma.intellij.plugin.updates

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class UrgentMessagesService(private val project: Project) : Disposable {

    private var urgentMessagesPanel: DigmaResettablePanel? = null

    private val myState = MyState()

    private val isMonitoringBackendVersion02234 = AtomicBoolean(false)

    companion object {
        fun getInstance(project: Project): UrgentMessagesService {
            return project.service<UrgentMessagesService>()
        }
    }

    override fun dispose() {
        //nothing to do, used as parent disposable
    }


    fun setPanel(urgentMessagesPanel: DigmaResettablePanel) {
        this.urgentMessagesPanel = urgentMessagesPanel
    }


    fun checkUrgentBackendUpdate() {
        if (isMonitoringBackendVersion02234.get()) {
            return
        }
        startMonitoringBackendVersion()
    }


    private fun isCurrentBackendVersion02234OrNewer(): Boolean {
        //don't catch API exceptions here. the monitoring routine should deal with exceptions
        val about = AnalyticsService.getInstance(project).about
        val backendVersion = ComparableVersion(about.applicationVersion)
        val urgentUpdateVersion = ComparableVersion("0.2.234")
        return backendVersion == urgentUpdateVersion || backendVersion.newerThan(urgentUpdateVersion)
    }

    @Synchronized //prevent multiple starts just in case. this method is not called many times so should not have impact on any performance
    private fun startMonitoringBackendVersion() {

        if (isMonitoringBackendVersion02234.get()) {
            return
        }
        isMonitoringBackendVersion02234.set(true)

        @Suppress("UnstableApiUsage")
        disposingScope().launch {

            //first check. if there is no connection to backend the result will be false and
            // monitoring will not start. this is called just after the update panel became visible so
            // most likely there is connection.
            myState.shouldShowUpgradeBackendMessage = try {
                !isCurrentBackendVersion02234OrNewer()
            } catch (e: Throwable) {
                false
            }

            //reset the panel no matter what the result is
            urgentMessagesPanel?.reset()

            //start monitoring. the message will stay on until backend is upgraded or project closed.
            while (isActive && myState.shouldShowUpgradeBackendMessage) {
                delay(10000)

                try {
                    if (isActive) {
                        if (isCurrentBackendVersion02234OrNewer()) {
                            myState.shouldShowUpgradeBackendMessage = false
                            urgentMessagesPanel?.reset()
                        }
                    }
                } catch (e: Throwable) {
                    ErrorReporter.getInstance().reportError("UrgentMessagesService.startMonitoringBackendVersion", e)
                }
            }

            isMonitoringBackendVersion02234.set(false)
            urgentMessagesPanel?.reset()
        }
    }


    fun shouldShowUpgradeBackendMessage(): Boolean {
        return myState.shouldShowUpgradeBackendMessage
    }


    private class MyState {
        var shouldShowUpgradeBackendMessage = false
    }

}