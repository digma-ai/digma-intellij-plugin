package org.digma.intellij.plugin.model.rest.insights

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.beans.ConstructorProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class InsightsStatsResult
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("dismissedCount", "issuesInsightsCount", "analyticsInsightsCount", "unreadInsightsCount", "criticalInsightsCount")
constructor(
    val dismissedCount: Number,
    val issuesInsightsCount: Int,
    val analyticsInsightsCount: Int,
    val unreadInsightsCount: Int,
    val criticalInsightsCount: Int
)
