package org.digma.intellij.plugin.ui.highlights

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.highlights.model.SetHighlightsPerformanceMessage
import org.digma.intellij.plugin.ui.highlights.model.SetHighlightsTopInsightsMessage
import org.digma.intellij.plugin.ui.jcef.BaseCommonMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.getQueryMapFromPayload
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript

class HighlightsMessageRouterHandler(project: Project) : BaseCommonMessageRouterHandler(project) {

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        when (action) {
            "MAIN/GET_HIGHLIGHTS_PERFORMANCE_DATA" -> getHighlightsPerformance(browser, requestJsonNode)
            "MAIN/GET_HIGHLIGHTS_TOP_ISSUES_DATA" -> getHighlightsTopInsights(browser, requestJsonNode)

            else -> {
                return false
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
        Log.log(logger::trace, project, "sending MAIN/GET_HIGHLIGHTS_PERFORMANCE_DATA message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }

    private fun getHighlightsTopInsights(browser: CefBrowser, requestJsonNode: JsonNode) {
        Log.log(logger::trace, project, "getHighlightsTopInsights called")

        val backendQueryParams = getQueryMapFromPayload(requestJsonNode, objectMapper)
        val payload = HighlightsService.getInstance(project).getHighlightsTopInsights(backendQueryParams)
        val message = SetHighlightsTopInsightsMessage(payload)
        Log.log(logger::trace, project, "sending MAIN/GET_HIGHLIGHTS_TOP_ISSUES_DATA message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }
}