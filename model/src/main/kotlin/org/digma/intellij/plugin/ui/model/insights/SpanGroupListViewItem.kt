package org.digma.intellij.plugin.ui.model.insights

class SpanGroupListViewItem(spanName: String) : InsightGroupListViewItem(spanName, InsightGroupType.Span) {

    fun getSpanName(): String {
        return groupId;
    }

}