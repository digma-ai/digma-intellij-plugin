package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*

data class ErrorsModel(
    var errorsCount: Int = 0,
    var methodName: String = "",
    var className: String = "",
    var listViewItems: List<ListViewItem<*>> = Collections.emptyList(),
    var scope: MethodInfo? = null
): PanelModel {


    override fun count(): String {
       return errorsCount.toString()
    }

    override fun classAndMethod(): String {
        return "$className.$methodName"
    }

    override fun isMethodScope(): Boolean {
        return true
    }
}
