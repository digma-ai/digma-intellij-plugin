package org.digma.intellij.plugin.ui.tests

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.InsightsModelReactHolder
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.executeWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload
import org.digma.intellij.plugin.ui.jcef.model.Payload
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.EndpointScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.insights.InsightsModelReact
import org.digma.intellij.plugin.ui.service.ScopeRequest
import org.digma.intellij.plugin.ui.service.TestsService
import org.digma.intellij.plugin.ui.tests.model.SetLatestTestsMessage
import java.util.Collections

class TestsMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {

    override fun getOriginForTroubleshootingEvent(): String {
        return "tests"
    }

    private fun model(): InsightsModelReact {
        return project.service<InsightsModelReactHolder>().model
    }

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {

        Log.log(logger::trace, project, "got action '$action' with message $requestJsonNode")

        when (action) {
            "TESTS/INITIALIZE" -> initialize(project, browser, requestJsonNode, rawRequest)
            "TESTS/SPAN_GET_LATEST_DATA" -> spanGetLatestData(project, browser, requestJsonNode, rawRequest)

            else -> {
                Log.log(logger::warn, "got unexpected action='$action'")
            }
        }
    }

    fun initialize(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String) {
        Log.log(logger::warn, "initialized'")
    }

    fun spanGetLatestData(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String) {
        val scopeRequest = buildScopeRequest()
        println("scopeRequest = $scopeRequest")
        if (scopeRequest == null) return

        Backgroundable.executeOnPooledThread {
            try {
                val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
                val pageNumber: Int = payloadNode.get("pageNumber").intValue()
                val pageSize: Int = payloadNode.get("pageSize").intValue()

                var environments: Set<String> = Collections.emptySet()
                val envsNode: JsonNode? = payloadNode.get("environments")
                if (envsNode != null && envsNode.isArray()) {
                    val envsArray = objectMapper.convertValue(envsNode, Array<String>::class.java)
                    environments = envsArray.toSet()
                }

                val testsOfSpanJson = project.service<TestsService>().getLatestTestsOfSpan(scopeRequest, environments, pageNumber, pageSize)

                Log.log(logger::trace, project, "got tests of span {}", testsOfSpanJson)
                val payload = objectMapper.readTree(testsOfSpanJson)
                val message = SetLatestTestsMessage("digma", "TESTS/SPAN_SET_LATEST_DATA", Payload(payload))
                Log.log(logger::trace, project, "sending TESTS/SPAN_SET_LATEST_DATA message")

                executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message))

            } catch (e: Exception) {
                Log.warnWithException(logger, e, "error setting tests of span data")
                var rethrow = true
                var errorDescription = e.toString()
                if (e is AnalyticsServiceException) {
                    errorDescription = e.getMeaningfulMessage()
                    rethrow = false
                }
                val message = SetLatestTestsMessage("digma", "TESTS/SPAN_SET_LATEST_DATA", Payload(null, ErrorPayload(errorDescription)))
                Log.log(logger::trace, project, "sending TESTS/SPAN_SET_LATEST_DATA message with error")
                executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message))
                ErrorReporter.getInstance().reportError(project, "TestsMessageRouterHandler.SPAN/SET_LATEST_DATA", e)
                //let BaseMessageRouterHandler handle the exception too in case it does something meaningful, worst case it will just log the error again
                if (rethrow) {
                    throw e
                }
            }
        } // Backgroundable
    }

    fun buildScopeRequest(): ScopeRequest? {
        val scope = model().scope

        val spans: MutableSet<String> = mutableSetOf()
        var methodCodeObjectId: String? = null
        var endpointCodeObjectId: String? = null

        when (scope) {
            is MethodScope -> {
                val methodInfo = scope.getMethodInfo()
                methodCodeObjectId = methodInfo.idWithType()
                if (methodInfo.hasRelatedCodeObjectIds()) {
                    spans.addAll(methodInfo.spans.map { it.idWithType() })

                    endpointCodeObjectId = methodInfo.endpoints.firstNotNullOf {
                        it.idWithType()
                    }
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

        return ScopeRequest(spans, methodCodeObjectId, endpointCodeObjectId)
    }

}
