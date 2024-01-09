package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.InsightsModelReactHolder
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.EndpointScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.Scope
import org.digma.intellij.plugin.ui.service.FillerOfLatestTests
import org.digma.intellij.plugin.ui.service.FilterForLatestTests
import org.digma.intellij.plugin.ui.service.ScopeRequest
import org.digma.intellij.plugin.ui.service.TestsService

class TestsServiceImpl(val project: Project) : TestsService {

    private val logger = Logger.getInstance(this::class.java)

    private var cefBrowser: CefBrowser? = null
    private var fillerOfLatestTests: FillerOfLatestTests? = null

    override fun dispose() {
        //nothing to do
    }

    override fun initWith(cefBrowser: CefBrowser, fillerOfLatestTests: FillerOfLatestTests) {
        this.cefBrowser = cefBrowser
        this.fillerOfLatestTests = fillerOfLatestTests
    }

    private fun scope(): Scope {
        return project.service<InsightsModelReactHolder>().model.scope
    }

    override fun refresh() {
        val scopeRequest = getScopeRequest()
        scopeRequest?.let {
            fillerOfLatestTests!!.fillDataOfTests(cefBrowser!!, it)
        }
    }

    override fun getScopeRequest(): ScopeRequest? {
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

            else -> {
                return null
            }
        }

        val scopeRequest = ScopeRequest(spans, methodCodeObjectId, endpointCodeObjectId)
//        println("DBG: scopeRequest=$scopeRequest")
        return scopeRequest
    }

    // return JSON as string (type LatestTestsOfSpanResponse)
    override fun getLatestTestsOfSpan(scopeRequest: ScopeRequest, filter: FilterForLatestTests): String {
        try {
            val json = project.service<AnalyticsService>().getLatestTestsOfSpan(scopeRequest, filter)
            return json
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "exception in getLatestTestsOfSpan")
            ErrorReporter.getInstance().reportError(project, "TestsService.getLatestTestsOfSpan", e)
            throw e
        }
    }

}