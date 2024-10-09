package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.jaegerui.JaegerUIService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.service.EditorService


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

    fun getErrorDetails(errorId: String): String {
        try {
            return AnalyticsService.getInstance(project).getErrorDetails(errorId)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error fetching error details")
            return "{}"
        }
    }

    fun getWorkspaceUris(methodCodeObjectIds: List<String>): Map<String, String> {
        try {
            return LanguageService.findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(project, methodCodeObjectIds)
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
