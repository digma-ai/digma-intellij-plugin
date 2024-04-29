package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.databind.JsonNode
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants


data class SetInsightStatsMessage(val payload: SetInsightStatsMessagePayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_INSIGHT_STATS
}

data class SetInsightStatsMessagePayload(
    val scope: JsonNode?,
    val analyticsInsightsCount: Number,
    val totalQueryResultCount: Number,
    val unreadInsightsCount: Number,
    val criticalInsightsCount: Number,
    val allIssuesCount: Number
)