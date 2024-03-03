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
import org.digma.intellij.plugin.common.Backgroundable
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
        Backgroundable.ensurePooledThread {
            if (!isCurrentBackendVersion02234()) {
                myState.shouldShowUpgradeBackendMessage = true
                urgentMessagesPanel?.reset()
                startMonitoringBackendVersion()
            }
        }
    }


    private fun isCurrentBackendVersion02234(): Boolean {
        try {
            val about = AnalyticsService.getInstance(project).about
            val backendVersion = ComparableVersion(about.applicationVersion)
            val urgentUpdateVersion = ComparableVersion("0.2.234")
            return backendVersion == urgentUpdateVersion || backendVersion.newerThan(urgentUpdateVersion)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("UrgentMessagesService.isCurrentBackendVersion02234", e)
            return true
        }
    }


    private fun startMonitoringBackendVersion() {

        if (isMonitoringBackendVersion02234.get()) {
            return
        }
        isMonitoringBackendVersion02234.set(true)

        @Suppress("UnstableApiUsage")
        this.disposingScope().launch {
            var needsUpdate = true
            while (isActive && needsUpdate) {
                delay(10000)
                try {
                    if (isActive) {
                        if (isCurrentBackendVersion02234()) {
                            myState.shouldShowUpgradeBackendMessage = false
                            needsUpdate = false
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