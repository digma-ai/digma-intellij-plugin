package org.digma.intellij.plugin.analytics

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.util.messages.Topic

interface InsightStatsChangedEvent {
    companion object {
        @JvmStatic
        @Topic.ProjectLevel
        val INSIGHT_STATS_CHANGED_TOPIC: Topic<InsightStatsChangedEvent> = Topic.create(
            "INSIGHT STATS CHANGED",
            InsightStatsChangedEvent::class.java
        )
    }

    fun insightStatsChanged(scope: JsonNode?, analyticsInsightsCount: Int, issuesInsightsCount: Int, unreadInsightsCount: Int)
}
