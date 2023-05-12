package org.digma.intellij.plugin.analytics

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier

class BackendConnectionUtil(project: Project) {
    private val logger: Logger = Logger.getInstance(BackendConnectionUtil::class.java)

    private val backendConnectionMonitor: BackendConnectionMonitor
    private var environmentsSupplier: EnvironmentsSupplier

    companion object {
        @JvmStatic
        fun getInstance(project: Project): BackendConnectionUtil {
            return project.getService(BackendConnectionUtil::class.java)
        }
    }

    init {
        backendConnectionMonitor = project.getService(BackendConnectionMonitor::class.java)
        val analyticsService = AnalyticsService.getInstance(project)
        environmentsSupplier = analyticsService.environment
    }

    fun testConnectionToBackend(): Boolean {

        //refresh will run in the background.
        //if there is currently no connection, but connection will recover during this refresh call then
        //not sure backendConnectionMonitor will catch it in time.
        //the next call will return the latest connection status
        //but anyway if the connection will recover an environmentChanged event will fire and that should
        // be a hook to intentionally cause a contextChange event.
        Log.log(logger::debug,"Triggering environmentsSupplier.refresh")
        environmentsSupplier.refresh()

        return backendConnectionMonitor.isConnectionOk()
    }

}