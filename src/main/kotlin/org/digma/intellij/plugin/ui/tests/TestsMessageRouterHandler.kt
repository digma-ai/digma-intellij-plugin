package org.digma.intellij.plugin.ui.tests

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.env.EnvironmentsSupplier
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.tests.FilterForLatestTests
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.teststab.TestsRunner
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.list.insights.openJaegerFromRecentActivity
import org.digma.intellij.plugin.ui.list.insights.traceButtonName
import org.digma.intellij.plugin.ui.tests.TestsPanel.Companion.RUN_TEST_BUTTON_NAME
import java.util.Collections

class TestsMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {

    override fun getOriginForTroubleshootingEvent(): String {
        return "tests"
    }

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {

        Log.log(logger::trace, project, "got action '$action' with message $requestJsonNode")

        when (action) {
            "TESTS/INITIALIZE" -> initialize(browser)
            "TESTS/SPAN_GET_LATEST_DATA" -> handleQuerySpanGetLatestData(project, requestJsonNode)
            "TESTS/RUN_TEST" -> handleRunTest(project, requestJsonNode)
            "TESTS/GO_TO_TRACE" -> handleGoToTrace(project, requestJsonNode)
            "TESTS/GO_TO_SPAN_OF_TEST" -> handleGoToSpanOfTest(project, requestJsonNode)

            else -> {
                Log.log(logger::warn, "got unexpected action='$action'")
            }
        }
    }

    private fun initialize(browser: CefBrowser) {
        Log.log(logger::info, "got TESTS/INITIALIZE")
        doCommonInitialize(browser)
    }

    private fun handleQuerySpanGetLatestData(project: Project, requestJsonNode: JsonNode) {

        //inner local method
        fun buildFilterForLatestTests(payloadNode: JsonNode): FilterForLatestTests {
            val pageNumber: Int = payloadNode.get("pageNumber").intValue()

            var environments: Set<String> = Collections.emptySet()
            val envsNode: JsonNode? = payloadNode.get("environments")
            if (envsNode != null && envsNode.isArray) {
                val envsArray = objectMapper.convertValue(envsNode, Array<String>::class.java)
                environments = envsArray.toSet()
            }
            return FilterForLatestTests(environments, pageNumber)
        }

        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val filter = buildFilterForLatestTests(payloadNode)
        val spanScopeNode = payloadNode.get("scope")
        var spanScope:SpanScope? = null
        spanScopeNode?.let{
            spanScope = objectMapper.treeToValue(spanScopeNode, SpanScope::class.java)
        }
        project.service<TestsUpdater>().updateTestsData(spanScope, filter)
    }

    private fun handleRunTest(project: Project, requestJsonNode: JsonNode) {
        ActivityMonitor.getInstance(project).registerButtonClicked(MonitoredPanel.Tests, RUN_TEST_BUTTON_NAME)
        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val methodCodeObjectId = payloadNode.get("methodCodeObjectId").textValue()
        val methodId = CodeObjectsUtil.stripMethodPrefix(methodCodeObjectId)

        project.service<TestsRunner>().executeTestMethod(methodId)
    }

    private fun handleGoToSpanOfTest(project: Project, requestJsonNode: JsonNode) {

        ActivityMonitor.getInstance(project).registerSpanLinkClicked(MonitoredPanel.Tests)

        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val environment = payloadNode.get("environment").textValue()
        val spanCodeObjectId = payloadNode.get("spanCodeObjectId").textValue()

        Backgroundable.ensurePooledThread {
            val environmentsSupplier: EnvironmentsSupplier = AnalyticsService.getInstance(project).environment
            environmentsSupplier.setCurrent(environment, false) {
                ScopeManager.getInstance(project).changeScope(SpanScope(spanCodeObjectId))
            }
        }
    }

    private fun handleGoToTrace(project: Project, requestJsonNode: JsonNode) {
        ActivityMonitor.getInstance(project).registerButtonClicked(MonitoredPanel.Tests, traceButtonName)

        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val traceId = payloadNode.get("traceId").textValue().orEmpty()
        val displayName = payloadNode.get("displayName").textValue().orEmpty()
        val spanCodeObjectId = payloadNode.get("spanCodeObjectId").textValue().orEmpty()

        //openJaegerFromRecentActivity method name is historic, fits here too
        openJaegerFromRecentActivity(project, traceId, displayName, spanCodeObjectId)
    }
}
