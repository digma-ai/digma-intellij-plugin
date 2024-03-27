package org.digma.intellij.plugin.ui.highlights

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.insights.AbstractInsightsMessageRouterHandler
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.highlights.model.SetHighlightsPerformanceMessage
import org.digma.intellij.plugin.ui.jcef.getQueryMapFromPayload
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript

class HighlightsMessageRouterHandler(project: Project) : AbstractInsightsMessageRouterHandler(project) {

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        when (action) {
            "HIGHLIGHTS/GET_PERFORMANCE" -> getHighlightsPerformance(browser, requestJsonNode)

            else -> {
                return super.doOnQuery(project, browser, requestJsonNode, rawRequest, action)
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
        Log.log(logger::trace, project, "sending HIGHLIGHTS/SET_PERFORMANCE message")
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }
}