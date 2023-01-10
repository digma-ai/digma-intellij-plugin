package org.digma.intellij.plugin.model.rest.recentactivity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.digma.intellij.plugin.model.rest.insights.Duration
import java.beans.ConstructorProperties

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
        val latestTraceTimestamp: String,
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
@ConstructorProperties("type", "codeObjectIds")
constructor(
        val type: String,
        val codeObjectIds: List<String>
)

data class RecentActivityGoToSpanRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("action", "payload")
constructor(
        val action: String,
        val payload: RecentActivityEntrySpanPayload?
)

data class RecentActivityEntrySpanPayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("span")
constructor(
        val span: EntrySpan
)

data class RecentActivityEntrySpanForTracePayload
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("traceId", "span")
constructor(
        val traceId: String,
        val span: EntrySpan
)

data class RecentActivityGoToTraceRequest
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
@JsonIgnoreProperties(ignoreUnknown = true)
@ConstructorProperties("action", "payload")
constructor(
        val action: String,
        val payload: RecentActivityEntrySpanForTracePayload?
)