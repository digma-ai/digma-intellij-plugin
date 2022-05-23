package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

open class InsightListViewItem<INSIGHT : CodeObjectInsight>(insight: INSIGHT) :
    ListViewItem<INSIGHT>(insight, evalSortIndex(insight)) {

    companion object {
        fun evalSortIndex(insight: CodeObjectInsight): Int {
            when (insight.type) {
                InsightType.HotSpot -> return 1
                InsightType.Errors -> return 2
                InsightType.SpanUsages -> return 30
                InsightType.SlowestSpans -> return 40
                InsightType.LowUsage -> return 50
                InsightType.NormalUsage -> return 60
                InsightType.HighUsage -> return 70
                InsightType.SlowEndpoint -> return 80
                else -> return 99
            }
        }
    }
}