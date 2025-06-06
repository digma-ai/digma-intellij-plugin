package org.digma.intellij.plugin.ui.highlights

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.project.Project
import org.apache.maven.artifact.versioning.ComparableVersion
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.getBackendVersion
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.highlights.HighlightsRequest
import org.digma.intellij.plugin.ui.highlights.model.*
import org.digma.intellij.plugin.ui.jcef.BaseCommonMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.getQueryMapFromPayload
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript

class HighlightsMessageRouterHandler(project: Project) : BaseCommonMessageRouterHandler(project) {

    override suspend fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        val version = getBackendVersion(project)
        val comparableVersion = ComparableVersion(version)
        if (version == "unknown" || comparableVersion.newerThan(ComparableVersion("0.3.7"))) {
            when (action) {
                "MAIN/GET_HIGHLIGHTS_PERFORMANCE_DATA" -> getHighlightsPerformanceV2(browser, requestJsonNode)
                "MAIN/GET_HIGHLIGHTS_TOP_ISSUES_DATA" -> getHighlightsTopInsightsV2(browser, requestJsonNode)
                "MAIN/GET_HIGHLIGHTS_IMPACT_DATA" -> getHighlightsImpact(browser, requestJsonNode)
                "MAIN/GET_HIGHLIGHTS_SCALING_DATA" -> getHighlightsScaling(browser, requestJsonNode)
                "MAIN/GET_SPAN_INFO_DATA" -> getSpanInfo(browser, requestJsonNode)

                else -> {
                    return false
                }
            }
        }
        else {
            when (action) {
                "MAIN/GET_HIGHLIGHTS_PERFORMANCE_DATA" -> getHighlightsPerformance(browser, requestJsonNode)
                "MAIN/GET_HIGHLIGHTS_TOP_ISSUES_DATA" -> getHighlightsTopInsights(browser, requestJsonNode)

                else -> {
                    return false
                }
            }
        }

        return true
    }

    @Synchronized
    @Throws(JsonProcessingException::class)
    private fun getHighlightsPerformance(browser: CefBrowser, requestJsonNode: JsonNode) {

        Log.log(logger::trace, project, "getHighlightsPerformance called")

        val backendQueryParams = getQueryMapFromPayload(requestJsonNode, objectMapper)
        val payload = HighlightsService.getInstance(project).getHighlightsPerformance(backendQueryParams)
        val message = SetHighlightsPerformanceMessage(payload)
        Log.log(logger::trace, project, "sending MAIN/SET_HIGHLIGHTS_PERFORMANCE_DATA message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }

    private fun getHighlightsTopInsights(browser: CefBrowser, requestJsonNode: JsonNode) {
        Log.log(logger::trace, project, "getHighlightsTopInsights called")

        val backendQueryParams = getQueryMapFromPayload(requestJsonNode, objectMapper)
        val payload = HighlightsService.getInstance(project).getHighlightsTopInsights(backendQueryParams)
        val message = SetHighlightsTopInsightsMessage(payload)
        Log.log(logger::trace, project, "sending MAIN/SET_HIGHLIGHTS_TOP_ISSUES_DATA message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }

    @Synchronized
    @Throws(JsonProcessingException::class)
    private fun getHighlightsPerformanceV2(browser: CefBrowser, requestJsonNode: JsonNode) {

        Log.log(logger::trace, project, "getHighlightsPerformance called")

        val payloadQuery = getPayloadQuery(requestJsonNode)
        if (payloadQuery is ObjectNode) {

            val request = createHighlightsRequest(payloadQuery, "scopedSpanCodeObjectId")
            val payload = HighlightsService.getInstance(project).getHighlightsPerformanceV2(request)

            val message = SetHighlightsPerformanceMessage(payload)
            Log.log(logger::trace, project, "sending MAIN/SET_HIGHLIGHTS_PERFORMANCE_DATA message")
            serializeAndExecuteWindowPostMessageJavaScript(browser, message)
        }
    }

    @Synchronized
    @Throws(JsonProcessingException::class)
    private fun getHighlightsImpact(browser: CefBrowser, requestJsonNode: JsonNode) {

        Log.log(logger::trace, project, "getHighlightsImpact called")

        val payloadQuery = getPayloadQuery(requestJsonNode)
        if (payloadQuery is ObjectNode) {

            val request = createHighlightsRequest(payloadQuery, "scopedSpanCodeObjectId")
            val payload = HighlightsService.getInstance(project).getHighlightsImpact(request)

            val message = SetHighlightsImpactMessage(payload)
            Log.log(logger::trace, project, "sending MAIN/SET_HIGHLIGHTS_IMPACT_DATA message")
            serializeAndExecuteWindowPostMessageJavaScript(browser, message)
        }
    }


    @Synchronized
    @Throws(JsonProcessingException::class)
    private fun getHighlightsScaling(browser: CefBrowser, requestJsonNode: JsonNode) {

        Log.log(logger::trace, project, "getHighlightsScaling called")

        val payloadQuery = getPayloadQuery(requestJsonNode)
        if (payloadQuery is ObjectNode) {

            val request = createHighlightsRequest(payloadQuery, "scopedSpanCodeObjectId")
            val payload = HighlightsService.getInstance(project).getHighlightsScaling(request)

            val message = SetHighlightsScalingMessage(payload)
            Log.log(logger::trace, project, "sending MAIN/SET_HIGHLIGHTS_PERFORMANCE_DATA message")
            serializeAndExecuteWindowPostMessageJavaScript(browser, message)
        }
    }

    @Synchronized
    @Throws(JsonProcessingException::class)
    private fun getSpanInfo(browser: CefBrowser, requestJsonNode: JsonNode) {

        Log.log(logger::trace, project, "getSpanInfo called")

        val payloadQuery = getPayloadQuery(requestJsonNode)
        if (payloadQuery is ObjectNode) {

            val request = payloadQuery.get("spanCodeObjectId").asText()
            val payload = HighlightsService.getInstance(project).getSpanInfo(request)

            val message = SetSpanInfoMessage(payload)
            Log.log(logger::trace, project, "sending MAIN/SET_SPAN_INFO_DATA message")
            serializeAndExecuteWindowPostMessageJavaScript(browser, message)
        }
    }


    private fun getHighlightsTopInsightsV2(browser: CefBrowser, requestJsonNode: JsonNode) {
        Log.log(logger::trace, project, "getHighlightsTopInsights called")

        val payloadQuery = getPayloadQuery(requestJsonNode)
        if (payloadQuery is ObjectNode) {

            val request = createHighlightsRequest(payloadQuery, "scopedCodeObjectId")
            val payload = HighlightsService.getInstance(project).getHighlightsTopInsightsV2(request)

            val message = SetHighlightsTopInsightsMessage(payload)
            Log.log(logger::trace, project, "sending MAIN/SET_HIGHLIGHTS_TOP_ISSUES_DATA message")
            serializeAndExecuteWindowPostMessageJavaScript(browser, message)
        }
    }

    private fun createHighlightsRequest(payloadQuery: ObjectNode, objectIdName: String): HighlightsRequest{
        val scopedCodeObjectId = payloadQuery.get(objectIdName).asText()
        val environmentsJsonArray = payloadQuery["environments"] as ArrayNode
        val environments = mutableListOf<String>()
        environmentsJsonArray.forEach { environment: JsonNode ->
            environments.add(environment.asText())
        }

        val highlightsRequest = HighlightsRequest(scopedCodeObjectId, environments)

        return highlightsRequest
    }

    private fun getPayloadQuery(requestJsonNode: JsonNode): JsonNode{
        val payloadNode: JsonNode = objectMapper.readTree(requestJsonNode.get("payload").toString())
        val payloadQuery: JsonNode = objectMapper.readTree(payloadNode.get("query").toString())

        return payloadQuery
    }
}