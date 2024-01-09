package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log

@Service(Service.Level.PROJECT)
class TestsService(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    override fun dispose() {
        //nothing to do
    }

    // return JSON as string (type LatestTestsOfSpanResponse)
    fun getLatestTestsOfSpan(spanCodeObjectId: String, environments: Set<String>, pageNumber: Int, pageSize: Int): String {
        try {
            val json = project.service<AnalyticsService>()
                .getLatestTestsOfSpan(setOf(spanCodeObjectId), environments, pageNumber, pageSize)
            return json
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "exception in getLatestTestsOfSpan")
            ErrorReporter.getInstance().reportError(project, "TestsService.getLatestTestsOfSpan", e)
            throw e
        }
    }

}