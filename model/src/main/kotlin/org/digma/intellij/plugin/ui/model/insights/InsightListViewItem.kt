package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

open class InsightListViewItem<INSIGHT : CodeObjectInsight>(insight: INSIGHT) :
    ListViewItem<INSIGHT>(insight, evalSortIndex(insight)) {

    companion object {
        fun evalSortIndex(insight: CodeObjectInsight): Int {
            return when (insight.type) {
                InsightType.HotSpot -> 0
                InsightType.Errors -> 1
                InsightType.SpanUsages -> 10
                InsightType.SlowestSpans -> 10
                InsightType.LowUsage -> 10
                InsightType.NormalUsage -> 10
                InsightType.HighUsage -> 10
                InsightType.SlowEndpoint -> 10
                else -> 99
            }
        }
    }
}