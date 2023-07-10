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

        //if called on background thread refreshNowOnBackground will run on the same thread ,
        // otherwise refreshNowOnBackground will run on background and isConnectionOk will return old result,
        // next call will be ok
        Log.log(logger::debug, "Triggering environmentsSupplier.refresh")
        environmentsSupplier.refreshNowOnBackground()

        return backendConnectionMonitor.isConnectionOk()
    }

}