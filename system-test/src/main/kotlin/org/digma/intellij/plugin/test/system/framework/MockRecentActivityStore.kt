package org.digma.intellij.plugin.test.system.framework

import org.digma.intellij.plugin.model.rest.insights.Duration
import org.digma.intellij.plugin.model.rest.recentactivity.EntrySpan
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.model.rest.recentactivity.SlimAggregatedInsight
import java.util.Date

fun createRecentActivityResultEntry(environment: String): RecentActivityResponseEntry {

    val defaultEntrySpan = EntrySpan(
        displayText = "defaultDisplayText",
        serviceName = "defaultServiceName",
        scopeId = "defaultScopeId",
        spanCodeObjectId = "defaultSpanCodeObjectId",
        methodCodeObjectId = "defaultMethodCodeObjectId"
    )

    // Create default SlimAggregatedInsight
    val defaultSlimAggregatedInsight = SlimAggregatedInsight(
        type = "defaultType",
        importance = 0,
        codeObjectIds = listOf()
    )

    return RecentActivityResponseEntry(
        environment = environment,
        traceFlowDisplayName = "defaultName",
        firstEntrySpan = defaultEntrySpan,
        lastEntrySpan = null,
        latestTraceId = "defaultTraceId",
        latestTraceTimestamp = Date(),
        latestTraceDuration = Duration(0.11, "ms", 11000),
        slimAggregatedInsights = listOf(defaultSlimAggregatedInsight)
    )
}

fun createRecentActivityResult(): RecentActivityResult {
    val env1Entry = createRecentActivityResultEntry("env1_mock")
    val env2Entry = createRecentActivityResultEntry("env2_mock")

    return RecentActivityResult(
        accountId = null,
        entries = listOf(env1Entry, env2Entry)
    )
}
