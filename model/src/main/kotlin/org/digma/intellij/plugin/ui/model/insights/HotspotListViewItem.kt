package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.rest.insights.HotspotInsight

class HotspotListViewItem(
    insight: HotspotInsight, sortIndex: Int,
    val header: String,
    val content: String,
    val linkText: String,
    val linkUrl: String
) :
    InsightListViewItem<HotspotInsight>(insight, sortIndex) {

}