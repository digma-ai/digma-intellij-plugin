package org.digma.intellij.plugin.ui.jaegerui

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.ui.jaegerui.model.GoToSpanMessage
import org.digma.intellij.plugin.ui.jaegerui.model.SpansMessage
import org.digma.intellij.plugin.ui.jaegerui.model.SpansWithResolvedLocationMessage
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript

class JaegerUIMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {

    override fun getOriginForTroubleshootingEvent(): String {
        return "jaegerui"
    }

    override suspend fun doOnQuery(
        project: Project,
        browser: CefBrowser,
        requestJsonNode: JsonNode,
        rawRequest: String,
        action: String
    ): Boolean {

        when (action) {
            "GET_SPANS_DATA" -> getSpansData(browser, requestJsonNode)
            "GO_TO_SPAN" -> goToSpan(requestJsonNode)
            else -> {
                return false
            }
        }
        return true
    }

    private suspend fun goToSpan(requestJsonNode: JsonNode) {
        val goToSpanMessage = objectMapper.treeToValue(requestJsonNode, GoToSpanMessage::class.java)
        JaegerUIService.getInstance(project).navigateToCode(goToSpanMessage)
    }

    private suspend fun getSpansData(browser: CefBrowser, requestJsonNode: JsonNode) {
        val spansMessage = objectMapper.treeToValue(requestJsonNode, SpansMessage::class.java)
        val resolvedSpans =
            try {
                JaegerUIService.getInstance(project).getResolvedSpans(spansMessage)
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "Exception while resolving spans for GET_SPANS_DATA")
                ErrorReporter.getInstance().reportError("JaegerUIMessageRouterHandler.getSpansData", e)
                emptyMap()
            }
        val spansWithResolvedLocationMessage = SpansWithResolvedLocationMessage("digma", "SET_SPANS_DATA", resolvedSpans)
        serializeAndExecuteWindowPostMessageJavaScript(browser, spansWithResolvedLocationMessage)
    }

}