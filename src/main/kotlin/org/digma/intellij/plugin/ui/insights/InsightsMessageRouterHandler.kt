package org.digma.intellij.plugin.ui.insights

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.insights.AbstractInsightsMessageRouterHandler
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.insights.MarkInsightsAsReadScope
import org.digma.intellij.plugin.posthog.ActivityMonitor

class InsightsMessageRouterHandler(project: Project) : AbstractInsightsMessageRouterHandler(project) {

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        when (action) {

            //todo: move all cases from AbstractInsightsMessageRouterHandler to here and convert to kotlin

            "INSIGHTS/MARK_INSIGHT_TYPES_AS_VIEWED" -> markInsightsViewed(requestJsonNode)
            "INSIGHTS/MARK_AS_READ" -> markInsightsAsRead(requestJsonNode)
            "INSIGHTS/MARK_ALL_AS_READ" -> markAllInsightsAsRead(requestJsonNode)

            else -> {
                return super.doOnQuery(project, browser, requestJsonNode, rawRequest, action)
            }
        }

        return true
    }

    @Throws(JsonProcessingException::class)
    private fun markInsightsAsRead(requestJsonNode: JsonNode) {
        Log.log(LOGGER::trace, project, "got INSIGHTS/MARK_AS_READ message")

        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val insightIdsJsonArray = payload["insightIds"] as ArrayNode
            val insightIds = mutableListOf<String>()
            insightIdsJsonArray.forEach { insightIdNode: JsonNode ->
                val insightId = insightIdNode.asText()
                insightIds.add(insightId)
            }
            InsightsService.getInstance(project).markInsightsAsRead(insightIds)
        }
    }

    @Throws(JsonProcessingException::class)
    private fun markAllInsightsAsRead(requestJsonNode: JsonNode) {
        Log.log(LOGGER::trace, project, "got INSIGHTS/MARK_ALL_AS_READ message")

        getPayloadFromRequest(requestJsonNode)?.let { payload ->

            var scope = payload.get("scope")
            if (scope is NullNode) {
                InsightsService.getInstance(project).markAllInsightsAsRead(null, null);
            } else {
                val spanCodeObjectId = scope["spanCodeObjectId"].asText()
                val methodCodeObjectId = scope["methodId"].asText()
                val serviceName = scope["serviceName"].asText()
                val role = scope["role"].asText()
                InsightsService.getInstance(project)
                    .markAllInsightsAsRead(scope, MarkInsightsAsReadScope(spanCodeObjectId, methodCodeObjectId, serviceName, role))
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