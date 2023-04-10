package org.digma.intellij.plugin.analytics

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService
import org.digma.intellij.plugin.ui.service.SummaryViewService

class BackendConnectionUtil(project: Project) {
    private val logger: Logger = Logger.getInstance(BackendConnectionUtil::class.java)

    private val backendConnectionMonitor: BackendConnectionMonitor
    private var insightsViewService: InsightsViewService
    private var errorsViewService: ErrorsViewService
    private var summaryViewService: SummaryViewService
    private var environmentsSupplier: EnvironmentsSupplier

    companion object {
        @JvmStatic
        fun getInstance(project: Project): BackendConnectionUtil {
            return project.getService(BackendConnectionUtil::class.java)
        }
    }

    init {
        backendConnectionMonitor = project.getService(BackendConnectionMonitor::class.java)
        insightsViewService = project.getService(InsightsViewService::class.java)
        errorsViewService = project.getService(ErrorsViewService::class.java)
        summaryViewService = project.getService(SummaryViewService::class.java)
        val analyticsService = project.getService(AnalyticsService::class.java)
        environmentsSupplier = analyticsService.environment
    }

    private var hadConnectionError = false

    fun testConnectionToBackend(): Boolean {

        //refresh will run in the background.
        //if there is currently no connection, but connection will recover during this refresh call then
        //not sure backendConnectionMonitor will catch it so the contextChange flow may still block.
        //the next contextChange will pass.
        //but anyway if the connection will recover an environmentChanged event will fire and that should have some kind
        //of hook to intentionally cause a contextChange event.
        environmentsSupplier.refresh()

        //hadConnectionError helps to call contextEmptyNoConnection() only once on the first time that a connection error is discovered.
        //and there is no need to empty again or change the models and ui until the connection is back.
        return if (backendConnectionMonitor.isConnectionError()) {
            if (hadConnectionError) {
                Log.log(logger::debug, "Not Executing contextChanged because there is no connection to backend service")
                return false
            }
            contextEmptyNoConnection()
            hadConnectionError = true
            false
        } else {
            hadConnectionError = false
            true
        }
    }

    private fun contextEmptyNoConnection() {
        Log.log(logger::debug, "contextEmptyNoConnection called")
        insightsViewService.empty()
        errorsViewService.empty()
        summaryViewService.empty()
    }

}