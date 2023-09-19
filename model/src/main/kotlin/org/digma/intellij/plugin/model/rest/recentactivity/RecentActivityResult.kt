package org.digma.intellij.plugin.model.rest.recentactivity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.Duration
import java.beans.ConstructorProperties
import java.util.Date

/**
 * Contracts Definition.
 *
 * @see [Contracts Definition](https://docs.google.com/document/d/10W8dJHXXxVXe2kI52NQjlu9LNsT4lp94AUogQ64IZ58)
 */

data class RecentActivityResult
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("accountId", "entries")
constructor(
    val accountId: String?,
    val entries: List<RecentActivityResponseEntry>,
)

data class RecentActivityResponseEntry
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("environment", "traceFlowDisplayName", "firstEntrySpan", "lastEntrySpan", "latestTraceId", "latestTraceTimestamp", "latestTraceDuration", "slimAggregatedInsights")
constructor(
        val environment: String,
        val traceFlowDisplayName: String,
        val firstEntrySpan: EntrySpan,
        val lastEntrySpan: EntrySpan?,
        val latestTraceId: String,
        val latestTraceTimestamp: Date,
        val latestTraceDuration: Duration,
        val slimAggregatedInsights: List<SlimAggregatedInsight>
)


data class EntrySpan
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("displayText", "serviceName", "scopeId", "spanCodeObjectId", "methodCodeObjectId")
constructor(
        val displayText: String,
        val serviceName: String,
        val scopeId: String,
        val spanCodeObjectId: String,
        val methodCodeObjectId: String
)

data class SlimAggregatedInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("type", "importance", "codeObjectIds")
constructor(
        val type: String,
        val importance: Int, // value of zero means that backend is still not up to date(forward compatibility)
        val codeObjectIds: List<String>
)




