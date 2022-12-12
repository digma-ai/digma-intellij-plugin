package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem

open class InsightGroupListViewItem(groupId: String, val type: InsightGroupType, route: String) :
    GroupListViewItem(type.sortIndex, groupId, route) {
}