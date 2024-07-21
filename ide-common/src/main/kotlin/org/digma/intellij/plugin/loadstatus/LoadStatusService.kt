package org.digma.intellij.plugin.loadstatus

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.version.LoadStatusResponse
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.util.Date
import kotlin.time.Duration.Companion.minutes

@Service(Service.Level.PROJECT)
class LoadStatusService(private val project: Project) : Disposable {
    companion object {
        private val logger = Logger.getInstance(LoadStatusService::class.java)

        @JvmStatic
        fun getInstance(project: Project): LoadStatusService {
            return project.getService(LoadStatusService::class.java)
        }
    }

    var affectedPanel: DigmaResettablePanel? = null // late init
    var lastLoadStatus = LoadStatusResponse("", Date(0), false, "")

    init {
        disposingPeriodicTask("LoadStatusService.periodicAction", 1.minutes.inWholeMilliseconds, 1.minutes.inWholeMilliseconds, true) {
            try {
                periodicAction()
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "Exception in periodicAction")
                ErrorReporter.getInstance().reportError(project, "LoadStatusService.periodicAction", e)
            }
        }
    }

    private fun periodicAction() {

        if (!isProjectValid(project)) return

        val analyticsService = AnalyticsService.getInstance(project)

        try {
            val response = analyticsService.loadStatus
            if (!response.isEmpty) {
                lastLoadStatus = response.get()
                Log.log(logger::debug, "got load status response {}", lastLoadStatus)
            }
        } catch (e: Throwable) {
            Log.log(logger::debug, "AnalyticsServiceException for getLoadStatus: {}", e.message)
            lastLoadStatus = LoadStatusResponse("", Date(0), false, "")
        }

        EDT.ensureEDT {
            affectedPanel?.reset()
        }
    }

    override fun dispose() {
    }
}