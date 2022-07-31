package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

open class InsightListViewItem<INSIGHT : CodeObjectInsight>(insight: INSIGHT) :
    ListViewItem<INSIGHT>(insight, sortIndexOf(insight.type)) {

    companion object {
        fun sortIndexOf(insightType: InsightType): Int {
            return when (insightType) {
                // Standalone insights
                InsightType.HotSpot -> 1
                InsightType.Errors -> 2
                InsightType.TopErrorFlows -> 3
                // Span
                InsightType.SpanUsages -> 60
                InsightType.SpanDurations -> 65
                InsightType.SpanDurationChange -> 66
                // HTTP Endpoints
                InsightType.SlowestSpans -> 40
                InsightType.LowUsage -> 30
                InsightType.NormalUsage -> 50
                InsightType.HighUsage -> 10
                InsightType.SlowEndpoint -> 20
                InsightType.Unmapped -> 200
            }
        }
    }
}