package org.digma.intellij.plugin.ui.errors

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.log.Log


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

    fun getErrorDetails(errorId: String): String {
        try {
            return AnalyticsService.getInstance(project).getErrorDetails(errorId)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error fetching error details")
            return "{}"
        }
    }


}
