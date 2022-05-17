package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.rest.insights.ErrorInsight

class ErrorsListViewItem(insight: ErrorInsight, sortIndex: Int, groupId: String) :
    InsightListViewItem<ErrorInsight>(insight, sortIndex, groupId) {

}