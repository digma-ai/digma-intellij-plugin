package org.digma.intellij.plugin.model.rest.recentactivity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.common.Duration
import java.beans.ConstructorProperties
import java.util.Date


@JsonIgnoreProperties(ignoreUnknown = true)
data class RecentActivityResult
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("accountId", "entries")
constructor(
    val accountId: String?,
    val entries: List<RecentActivityResponseEntry>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RecentActivityResponseEntry
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
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


@JsonIgnoreProperties(ignoreUnknown = true)
data class EntrySpan
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("displayText", "serviceName", "scopeId", "spanCodeObjectId", "methodCodeObjectId")
constructor(
        val displayText: String,
        val serviceName: String?,
        val scopeId: String,
        val spanCodeObjectId: String?,
        val methodCodeObjectId: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlimAggregatedInsight
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@ConstructorProperties("type", "importance", "codeObjectIds")
constructor(
        val type: String,
        val importance: Int, // value of zero means that backend is still not up to date(forward compatibility)
        val codeObjectIds: List<String>
)




