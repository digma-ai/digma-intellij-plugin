package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

open class InsightListViewItem<INSIGHT : CodeObjectInsight>(insight: INSIGHT) :
    ListViewItem<INSIGHT>(insight, sortIndexOf(insight.type)) {

    val insightType get() = modelObject.type

    companion object {
        fun sortIndexOf(insightType: InsightType): Int {
            return when (insightType) {
                // Standalone insights
                InsightType.HotSpot -> 1
                InsightType.Errors -> 2
                InsightType.TopErrorFlows -> 3
                // Span
                InsightType.SpanDurations -> 60
                InsightType.SpanUsages -> 61
                InsightType.SpanScalingRootCause -> 62
                InsightType.SpanScaling -> 63
                InsightType.SpaNPlusOne -> 65
                InsightType.SpanDurationChange -> 66
                InsightType.SpanEndpointBottleneck -> 67
                InsightType.SpanDurationBreakdown -> 68
                // HTTP Endpoints
                InsightType.EndpointSpaNPlusOne -> 55
                InsightType.EndpointSessionInView -> 56

                InsightType.SlowestSpans -> 40
                InsightType.LowUsage -> 30
                InsightType.NormalUsage -> 50
                InsightType.HighUsage -> 10
                InsightType.SlowEndpoint -> 20
                InsightType.EndpointDurationSlowdown -> 25 //??? where it should be ?
                InsightType.EndpointBreakdown -> 5
                InsightType.Unmapped -> 200
            }
        }
    }
}