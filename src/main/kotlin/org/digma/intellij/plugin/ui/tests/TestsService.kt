package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.InsightsModelReact
import org.digma.intellij.plugin.insights.InsightsScopeChangeListener
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.tests.FilterForLatestTests
import org.digma.intellij.plugin.model.rest.tests.TestsScopeRequest
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.EndpointScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.Scope

@Service(Service.Level.PROJECT)
class TestsService(val project: Project) : Disposable, InsightsScopeChangeListener {

    private val logger = Logger.getInstance(this::class.java)

    private var pageSize: Int = 10

    init {
        InsightsModelReact.getInstance(project).addScopeChangeListener(this, this)
    }


    override fun scopeChanged(scope: Scope) {
        updateTests()
    }

    override fun dispose() {
        InsightsModelReact.getInstance(project).removeChangeListener(this)
    }


    private fun scope(): Scope {
        return InsightsModelReact.getInstance(project).scope
    }

    private fun updateTests() {
        Backgroundable.ensurePooledThread {
            val scopeRequest = getScopeRequest()
            project.service<TestsUpdater>().updateTestsData(scopeRequest)
        }
    }


    fun getScopeRequest(): TestsScopeRequest {
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

        return TestsScopeRequest(spans, methodCodeObjectId, endpointCodeObjectId)
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
