package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem

open class InsightGroupListViewItem(groupId: String, val type: InsightGroupType) :
    GroupListViewItem(type.sortIndex, groupId) {
}