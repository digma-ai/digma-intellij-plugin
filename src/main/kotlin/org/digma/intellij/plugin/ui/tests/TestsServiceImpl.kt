package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.InsightsModelReactHolder
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.EndpointScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.Scope
import org.digma.intellij.plugin.ui.service.FilterForLatestTests
import org.digma.intellij.plugin.ui.service.ScopeRequest
import org.digma.intellij.plugin.ui.service.TestsService

class TestsServiceImpl(val project: Project) : TestsService {

    private val logger = Logger.getInstance(this::class.java)

    private var pageSize: Int = 20

    override fun dispose() {
        //nothing to do
    }

    override fun setPageSize(pageSize: Int) {
        Log.log(logger::info, "initialized with pageSize {}", pageSize)
        this.pageSize = pageSize
    }


    private fun scope(): Scope {
        return project.service<InsightsModelReactHolder>().model.scope
    }

    override fun refresh() {
        Backgroundable.ensurePooledThread {
            val scopeRequest = getScopeRequest()
            project.service<TestsUpdater>().updateTestsData(scopeRequest)
        }
    }


    override fun getScopeRequest(): ScopeRequest {
        val scope = scope()

        val spans: MutableSet<String> = mutableSetOf()
        var methodCodeObjectId: String? = null
        var endpointCodeObjectId: String? = null

        when (scope) {
            is MethodScope -> {
                val methodInfo = scope.getMethodInfo()
                methodCodeObjectId = methodInfo.idWithType()
                if (methodInfo.hasRelatedCodeObjectIds()) {
                    spans.addAll(methodInfo.spans.map { it.idWithType() })
                    endpointCodeObjectId = methodInfo.endpoints.firstOrNull()?.idWithType()
                }
            }

            is CodeLessSpanScope -> {
                spans.add(CodeObjectsUtil.addSpanTypeToId(scope.getSpan().spanId))
            }

            is EndpointScope -> {
                endpointCodeObjectId = CodeObjectsUtil.addEndpointTypeToId(scope.getEndpoint().id)
            }
        }

        return ScopeRequest(spans, methodCodeObjectId, endpointCodeObjectId)
    }


    // return JSON as string (type LatestTestsOfSpanResponse)
    override fun getLatestTestsOfSpan(scopeRequest: ScopeRequest, filter: FilterForLatestTests): String {
        try {
            return AnalyticsService.getInstance(project).getLatestTestsOfSpan(scopeRequest, filter, pageSize)
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "exception in getLatestTestsOfSpan")
            ErrorReporter.getInstance().reportError(project, "TestsService.getLatestTestsOfSpan", e)
            throw e
        }
    }

}