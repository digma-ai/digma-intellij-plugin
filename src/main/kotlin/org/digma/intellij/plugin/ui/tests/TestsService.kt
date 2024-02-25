package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.tests.FilterForLatestTests
import org.digma.intellij.plugin.model.rest.tests.TestsScopeRequest

@Service(Service.Level.PROJECT)
class TestsService(val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var pageSize: Int = 10

    override fun dispose() {
        //nothing to do, used as parent disposable
    }

    // return JSON as string (type LatestTestsOfSpanResponse)
    fun getLatestTestsOfSpan(scopeRequest: TestsScopeRequest, filter: FilterForLatestTests): String {
        try {
            return AnalyticsService.getInstance(project).getLatestTestsOfSpan(scopeRequest, filter, pageSize)
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "exception in getLatestTestsOfSpan")
            ErrorReporter.getInstance().reportError(project, "TestsService.getLatestTestsOfSpan", e)
            throw e
        }
    }


}
