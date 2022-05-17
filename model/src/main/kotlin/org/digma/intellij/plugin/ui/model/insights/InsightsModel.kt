package org.digma.intellij.plugin.ui.model.insights

import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*

data class InsightsModel(
    var insightsCount: Int = 0,
    var methodName: String = "",
    var className: String = "",
    var listViewItems: List<ListViewItem> = Collections.emptyList()
) : PanelModel {

    override fun count(): String {
        return insightsCount.toString()
    }

    override fun classAndMethod(): String {
        return "$className.$methodName"
    }

    override fun isMethodScope(): Boolean {
        return true
    }
}
