package org.digma.intellij.plugin.ui.insights

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.insights.AbstractInsightsMessageRouterHandler
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor

class InsightsMessageRouterHandler(project: Project) : AbstractInsightsMessageRouterHandler(project) {

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {

        when (action) {

            //todo: move all cases from AbstractInsightsMessageRouterHandler to here and convert to kotlin

            "INSIGHTS/MARK_INSIGHT_TYPES_AS_VIEWED" -> markInsightsViewed(requestJsonNode)

            else -> {
                super.doOnQuery(project, browser, requestJsonNode, rawRequest, action)
            }
        }
    }


    @Throws(JsonProcessingException::class)
    private fun markInsightsViewed(requestJsonNode: JsonNode) {
        Log.log(LOGGER::trace, project, "got INSIGHTS/MARK_INSIGHT_TYPES_AS_VIEWED message")

        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val insightsTypesJasonArray = payload["insightTypes"] as ArrayNode
            val insightTypeList = mutableListOf<Pair<String, Int>>()
            insightsTypesJasonArray.forEach { insightType: JsonNode ->
                val type = insightType["type"].asText()
                val reopensCount = insightType["reopenCount"]?.asInt() ?: 0
                val insightOpensCount = Pair(type, reopensCount)
                insightTypeList.add(insightOpensCount)
            }
            Log.log({ message: String? -> LOGGER.trace(message) }, project, "got insights types {}", insightTypeList)
            ActivityMonitor.getInstance(project).registerInsightsViewed(insightTypeList)
        }
    }

}