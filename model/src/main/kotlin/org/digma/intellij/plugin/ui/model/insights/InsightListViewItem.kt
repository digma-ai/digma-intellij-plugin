package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.ui.model.listview.ListViewItem

abstract class InsightListViewItem<INSIGHT : CodeObjectInsight>(insight: INSIGHT, sortIndex: Int, groupId: String) :
    ListViewItem<INSIGHT>(insight, sortIndex, groupId) {

}