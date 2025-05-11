package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.findWorkspaceUrisForCodeObjectIdsForErrorStackTrace
import org.digma.intellij.plugin.service.EditorService
import org.digma.intellij.plugin.ui.errors.model.ActionError
import org.digma.intellij.plugin.ui.errors.model.ErrorActionResult
import org.digma.intellij.plugin.ui.jaegerui.JaegerUIService


@Service(Service.Level.PROJECT)
class ErrorsService(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ErrorsService {
            return project.service<ErrorsService>()
        }
    }

    override fun dispose() {
        //nothing to do , used as parent disposable
    }

    fun getErrorsData(objectIds: List<String>): String {
        try {
            return AnalyticsService.getInstance(project).getErrors(objectIds)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error fetching errors")
            return "[]"
        }
    }

    fun getGlobalErrorsData(payload: String): String? {
        try {
            return AnalyticsService.getInstance(project).getGlobalErrors(payload)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error fetching errors")
            return null
        }
    }

    fun pinError(errorId: String, environment: String): ErrorActionResult {
        try {
            AnalyticsService.getInstance(project).pinError(errorId, environment)
            return ErrorActionResult(errorId, true, null)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "exception during pin error")
            return ErrorActionResult(errorId, false, ActionError(e.message))
        }
    }

    fun unpinError(errorId: String, environment: String): ErrorActionResult {
        try {
            AnalyticsService.getInstance(project).unpinError(errorId, environment)
            return ErrorActionResult(errorId, true, null)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "exception during unpin error")
            return ErrorActionResult(errorId, false, ActionError(e.message))
        }
    }

    fun dismissError(errorId: String, environment: String): ErrorActionResult {
        try {
            AnalyticsService.getInstance(project).dismissError(errorId, environment)
            return ErrorActionResult(errorId, true, null)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "exception during dismiss error")
            return ErrorActionResult(errorId, false, ActionError(e.message))
        }
    }

    fun undismissError(errorId: String, environment: String): ErrorActionResult {
        try {
            AnalyticsService.getInstance(project).undismissError(errorId, environment)
            return ErrorActionResult(errorId, true, null)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "exception during undismiss error")
            return ErrorActionResult(errorId, false, ActionError(e.message))
        }
    }

    fun getGlobalErrorsFiltersData(payload: String): String? {
        try {
            return AnalyticsService.getInstance(project).getGlobalErrorsFilters(payload)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error fetching errors/filters")
            return null
        }
    }

    fun getErrorTimeseries( errorId: String, payload:Map<String, Any>): String? {
        try {
            return AnalyticsService.getInstance(project).getErrorTimeseries(errorId, payload)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error fetching errors")
            return null
        }
    }

    fun getErrorDetails(errorId: String): String {
        try {
            return AnalyticsService.getInstance(project).getErrorDetails(errorId)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error fetching error details")
            return "{}"
        }
    }

    suspend fun getWorkspaceUris(methodCodeObjectIds: List<String>): Map<String, String> {
        try {
            return findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(project, methodCodeObjectIds)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error in getWorkspaceUrls")
            return mapOf()
        }
    }

    fun openErrorFrameWorkspaceFile(workspaceUrl: String, lineNumber: String, lastInstanceCommitId: String?) {
        EDT.ensureEDT {
            EditorService.getInstance(project).openErrorFrameWorkspaceFileInEditor(workspaceUrl, lastInstanceCommitId, lineNumber.toInt())
        }
    }


    fun openRawStackTrace(stackTrace: String) {
        EDT.ensureEDT {
            EditorService.getInstance(project).openRawStackTrace(stackTrace)
        }
    }

    fun openTrace(traceId: String, spanName: String, spanCodeObjectId: String?) {
        JaegerUIService.getInstance(project).openEmbeddedJaeger(traceId, spanName, spanCodeObjectId, true)
    }

}
