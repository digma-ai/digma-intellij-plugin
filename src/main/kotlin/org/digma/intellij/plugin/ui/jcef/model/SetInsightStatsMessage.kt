package org.digma.intellij.plugin.ui.jcef.model

import com.fasterxml.jackson.annotation.JsonCreator
import org.digma.intellij.plugin.ui.jcef.JCEFGlobalConstants
import java.beans.ConstructorProperties

data class SetInsightStatsMessage(val payload: SetInsightStatsMessagePayload) {
    val type = JCEFGlobalConstants.REQUEST_MESSAGE_TYPE
    val action = JCEFGlobalConstants.GLOBAL_SET_INSIGHT_STATS
}

data class SetInsightStatsMessagePayload(
    val scope: InsightStatsScope?,
    val analyticsInsightsCount: Number,
    val issuesInsightsCount: Number,
    val unreadInsightsCount: Number
)

data class InsightStatsScope
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties(
    "spanCodeObjectId"
)
constructor (
    val spanCodeObjectId: String
)