package org.digma.intellij.plugin.ui.tests

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils.REQUEST_MESSAGE_TYPE
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.teststab.TestsRunner
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.executeWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload
import org.digma.intellij.plugin.ui.jcef.model.Payload
import org.digma.intellij.plugin.ui.list.insights.openJaegerFromRecentActivity
import org.digma.intellij.plugin.ui.list.insights.traceButtonName
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import org.digma.intellij.plugin.ui.service.FillerOfLatestTests
import org.digma.intellij.plugin.ui.service.FilterForLatestTests
import org.digma.intellij.plugin.ui.service.ScopeRequest
import org.digma.intellij.plugin.ui.service.TestsService
import org.digma.intellij.plugin.ui.tests.TestsTabPanel.Companion.RunTestButtonName
import org.digma.intellij.plugin.ui.tests.model.SetLatestTestsMessage
import java.util.Collections

class TestsMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project), FillerOfLatestTests {

    private var lastKnownFilterForLatestTests: FilterForLatestTests = FilterForLatestTests(emptySet())

    override fun getOriginForTroubleshootingEvent(): String {
        return "tests"
    }

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {

        Log.log(logger::trace, project, "got action '$action' with message $requestJsonNode")

        when (action) {
            "TESTS/INITIALIZE" -> handleQueryInitialize(project, browser, requestJsonNode, rawRequest)
            "TESTS/SPAN_GET_LATEST_DATA" -> handleQuerySpanGetLatestData(project, browser, requestJsonNode, rawRequest)
            "TESTS/RUN_TEST" -> handleRunTest(project, browser, requestJsonNode, rawRequest)
            "TESTS/GO_TO_TRACE" -> handleGoToTrace(project, browser, requestJsonNode, rawRequest)
            "TESTS/GO_TO_SPAN_OF_TEST" -> handleGoToSpanOfTest(project, browser, requestJsonNode, rawRequest)

            else -> {
                Log.log(logger::warn, "got unexpected action='$action'")
            }
        }
    }

    fun handleQueryInitialize(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String) {
        Log.log(logger::info, "initializing")

        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val pageSize: Int = payloadNode.get("pageSize").intValue()

        val testsService = project.service<TestsService>()
        testsService.initWith(browser, this, pageSize)
        testsService.sendOperativeEnvironments()

        Log.log(logger::info, "initialized with pageSize=$pageSize")
    }

    private fun buildFilterForLatestTests(requestJsonNode: JsonNode): FilterForLatestTests {
        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val pageNumber: Int = payloadNode.get("pageNumber").intValue()

        var environments: Set<String> = Collections.emptySet()
        val envsNode: JsonNode? = payloadNode.get("environments")
        if (envsNode != null && envsNode.isArray()) {
            val envsArray = objectMapper.convertValue(envsNode, Array<String>::class.java)
            environments = envsArray.toSet()
        }
        return FilterForLatestTests(environments, pageNumber)
    }

    private fun handleQuerySpanGetLatestData(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String) {
        lastKnownFilterForLatestTests = buildFilterForLatestTests(requestJsonNode)

        val scopeRequest = project.service<TestsService>().getScopeRequest()
        if (scopeRequest == null) return

        fillDataOfTests(browser, scopeRequest)
    }

    override fun fillDataOfTests(cefBrowser: CefBrowser, scopeRequest: ScopeRequest) {
        Backgroundable.executeOnPooledThread {
            try {
                val testsOfSpanJson = project.service<TestsService>().getLatestTestsOfSpan(scopeRequest, lastKnownFilterForLatestTests)

                Log.log(logger::trace, project, "got tests of span {}", testsOfSpanJson)
                val payload = objectMapper.readTree(testsOfSpanJson)
                val message = SetLatestTestsMessage(REQUEST_MESSAGE_TYPE, "TESTS/SPAN_SET_LATEST_DATA", Payload(payload))
                Log.log(logger::trace, project, "sending TESTS/SPAN_SET_LATEST_DATA message")

                executeWindowPostMessageJavaScript(cefBrowser, objectMapper.writeValueAsString(message))
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "error setting tests of span data")
                var rethrow = true
                var errorDescription = e.toString()
                if (e is AnalyticsServiceException) {
                    errorDescription = e.getMeaningfulMessage()
                    rethrow = false
                }
                val message = SetLatestTestsMessage(REQUEST_MESSAGE_TYPE, "TESTS/SPAN_SET_LATEST_DATA", Payload(null, ErrorPayload(errorDescription)))
                Log.log(logger::trace, project, "sending TESTS/SPAN_SET_LATEST_DATA message with error")
                executeWindowPostMessageJavaScript(cefBrowser, objectMapper.writeValueAsString(message))
                ErrorReporter.getInstance().reportError(project, "TestsMessageRouterHandler.SPAN/SET_LATEST_DATA", e)
                //let BaseMessageRouterHandler handle the exception too in case it does something meaningful, worst case it will just log the error again
                if (rethrow) {
                    throw e
                }
            }
        } // Backgroundable
    }

    private fun handleRunTest(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String) {
        project.service<ActivityMonitor>().registerButtonClicked(MonitoredPanel.Tests, RunTestButtonName)
        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val methodCodeObjectId = payloadNode.get("methodCodeObjectId").textValue()
        val methodId = CodeObjectsUtil.stripMethodPrefix(methodCodeObjectId)

        project.service<TestsRunner>().executeTestMethod(methodId)
    }

    private fun handleGoToSpanOfTest(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String) {
        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val environment = payloadNode.get("environment").textValue()
        val spanCodeObjectId = payloadNode.get("spanCodeObjectId").textValue()
        val methodCodeObjectIdNode = payloadNode.get("methodCodeObjectId")

        var methodCodeObjectId: String? = null
        if (methodCodeObjectIdNode != null && !methodCodeObjectIdNode.isNull) {
            methodCodeObjectId = methodCodeObjectIdNode.textValue()
        }

        Backgroundable.ensurePooledThread {
            val environmentsSupplier: EnvironmentsSupplier = project.service<AnalyticsService>().environment
            environmentsSupplier.setCurrent(environment, false) {
                if (methodCodeObjectId != null) {
                    project.service<InsightsViewOrchestrator>().showInsightsForMethod(methodCodeObjectId)
                } else {
                    project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(spanCodeObjectId)
                }
            }
        }
    }

    private fun handleGoToTrace(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String) {
        project.service<ActivityMonitor>().registerButtonClicked(MonitoredPanel.Tests, traceButtonName)

        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val traceId = payloadNode.get("traceId").textValue().orEmpty()
        val displayName = payloadNode.get("displayName").textValue().orEmpty()
        val spanCodeObjectId = payloadNode.get("spanCodeObjectId").textValue().orEmpty()

        openJaegerFromRecentActivity(project, traceId, displayName, spanCodeObjectId)
    }
}
