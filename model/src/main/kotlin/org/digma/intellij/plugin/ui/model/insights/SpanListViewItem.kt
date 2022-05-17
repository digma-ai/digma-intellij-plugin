package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.rest.insights.SpanInsight

class SpanListViewItem(insight: SpanInsight, sortIndex: Int) :
    InsightListViewItem<SpanInsight>(insight, sortIndex, insight.span) {

}