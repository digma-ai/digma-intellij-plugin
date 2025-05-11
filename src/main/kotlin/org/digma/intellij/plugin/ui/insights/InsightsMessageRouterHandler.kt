package org.digma.intellij.plugin.ui.insights

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.digmathon.DigmathonService
import org.digma.intellij.plugin.insights.LegacyJavaInsightsMessageRouterHandler
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.insights.MarkInsightsAsReadScope
import org.digma.intellij.plugin.model.rest.navigation.SpanNavigationItem
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.insights.model.SetCodeLocationData
import org.digma.intellij.plugin.ui.insights.model.SetCodeLocationMessage
import org.digma.intellij.plugin.ui.jcef.BaseCommonMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityService
import java.util.TreeMap
import java.util.stream.Collectors

class InsightsMessageRouterHandler(project: Project) : BaseCommonMessageRouterHandler(project) {

    private val legacyJava = LegacyJavaInsightsMessageRouterHandler(project, this)

    override suspend fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        when (action) {

            //todo: move all cases from LegacyJavaInsightsMessageRouterHandler to here and convert to kotlin

            "INSIGHTS/MARK_INSIGHT_TYPES_AS_VIEWED" -> markInsightsViewed(requestJsonNode)
            "INSIGHTS/MARK_AS_READ" -> markInsightsAsRead(requestJsonNode)
            "INSIGHTS/MARK_ALL_AS_READ" -> markAllInsightsAsRead(requestJsonNode)
            "INSIGHTS/GET_CODE_LOCATIONS" -> getCodeLocations(browser, requestJsonNode)


            else -> {
                return legacyJava.doOnQueryJavaLegacy(browser, requestJsonNode, action)
            }
        }

        return true
    }

    @Throws(JsonProcessingException::class)
    private fun markInsightsAsRead(requestJsonNode: JsonNode) {
        Log.log(logger::trace, project, "got INSIGHTS/MARK_AS_READ message")

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
        Log.log(logger::trace, project, "got INSIGHTS/MARK_ALL_AS_READ message")

        getPayloadFromRequest(requestJsonNode)?.let { payload ->

            val scope = payload.get("scope")
            if (scope is NullNode) {
                InsightsService.getInstance(project).markAllInsightsAsRead(null, null)
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
        Log.log(logger::trace, project, "got INSIGHTS/MARK_INSIGHT_TYPES_AS_VIEWED message")

        getPayloadFromRequest(requestJsonNode)?.let { payload ->
            val insightsTypesJasonArray = payload["insightTypes"] as ArrayNode
            val insightTypeList = mutableListOf<Pair<String, Int>>()
            insightsTypesJasonArray.forEach { insightType: JsonNode ->
                val type = insightType["type"].asText()
                val reopensCount = insightType["reopenCount"]?.asInt() ?: 0
                val insightOpensCount = Pair(type, reopensCount)
                insightTypeList.add(insightOpensCount)
            }
            Log.log({ message: String? -> logger.trace(message) }, project, "got insights types {}", insightTypeList)
            ActivityMonitor.getInstance(project).registerInsightsViewed(insightTypeList)
            if (DigmathonService.getInstance().isUserActive()) {
                DigmathonService.getInstance().addInsightsViewed(insightTypeList.map { it.first })
                //stop sending this message if the user finished the digmathon
                if (!DigmathonService.getInstance().isUserFinishedDigmathon) {
                    RecentActivityService.getInstance(project)
                        .setDigmathonProgressData(
                            DigmathonService.getInstance().viewedInsights,
                            DigmathonService.getInstance().getDigmathonInsightsViewedLastUpdated()
                        )
                }
            }
        }
    }

    @Throws(JsonProcessingException::class)
    private suspend fun getCodeLocations(browser: CefBrowser, jsonNode: JsonNode) {
        val codeLocations = try {
            buildCodeLocations(jsonNode)
        } catch (e: Exception) {
            Log.warnWithException(logger, project, e, "unhandled error in getCodeLocations: {}", e.message)
            listOf()
        }
        setCodeLocations(browser, codeLocations)
    }


    private fun setCodeLocations(browser: CefBrowser, codeLocations: List<String>) {
        val message = SetCodeLocationMessage("digma", "INSIGHTS/SET_CODE_LOCATIONS", SetCodeLocationData(codeLocations))
        serializeAndExecuteWindowPostMessageJavaScript(browser, message)
    }


    private suspend fun buildCodeLocations(jsonNode: JsonNode): List<String> {
        Log.log(logger::trace, project, "got INSIGHTS/GET_CODE_LOCATIONS message")
        val payload = objectMapper.readTree(jsonNode.get("payload").toString())
        val spanCodeObjectId = payload.get("spanCodeObjectId").asText()
        val methodCodeObjectIdNode = payload.get("methodCodeObjectId")
        val codeLocations = mutableListOf<String>()
        if (methodCodeObjectIdNode != null) {
            val methodCodeObjectId = methodCodeObjectIdNode.asText()
            if (methodCodeObjectId != null && !methodCodeObjectId.isEmpty()) {
                codeLocations.add(getMethodFQN(methodCodeObjectId))
                return codeLocations
            }
        }

        var methodCodeObjectId = CodeNavigator.getInstance(project).findMethodCodeObjectId(spanCodeObjectId)
        if (methodCodeObjectId != null) {
            codeLocations.add(getMethodFQN(methodCodeObjectId))
            return codeLocations
        }


        val codeObjectNavigation = AnalyticsService.getInstance(project).getCodeObjectNavigation(spanCodeObjectId)
        val closestParentSpans: List<SpanNavigationItem> = codeObjectNavigation.navigationEntry.closestParentSpans
        val distancedMap = TreeMap(closestParentSpans.stream().collect(Collectors.groupingBy(SpanNavigationItem::distance)))
        for (entry in distancedMap.entries) { //exit when code location found sorted by distance.
            val navigationItems: MutableList<SpanNavigationItem> = entry.value
            for (navigationItem in navigationItems) {
                methodCodeObjectId = navigationItem.methodCodeObjectId
                if (methodCodeObjectId == null) { //no method code object attached to span, try using client side discovery
                    methodCodeObjectId = CodeNavigator.getInstance(project).findMethodCodeObjectId(navigationItem.spanCodeObjectId)
                }
                if (methodCodeObjectId != null) {
                    codeLocations.add(getMethodFQN(methodCodeObjectId))
                }
            }
        }

        return codeLocations
    }


    fun getMethodFQN(methodCodeObjectId: String): String {
        val pair = CodeObjectsUtil.getMethodClassAndName(methodCodeObjectId)
        val classFqn = pair.first
        val methodName = pair.second
        return String.format("%s.%s", classFqn, methodName)
    }
}