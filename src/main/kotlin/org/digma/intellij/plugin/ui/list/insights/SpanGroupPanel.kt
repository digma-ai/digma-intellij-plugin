package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.model.rest.insights.SpanInsight
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Color
import java.util.*
import javax.swing.JPanel

fun spanGroupPanel(listViewItem: GroupListViewItem): JPanel {

    val items: SortedSet<ListViewItem<*>> = listViewItem.modelObject

    val result = panel {
        row("Span:"+listViewItem.groupId) {}
        items.forEach {
            row {
                cell(spanPanel(it as ListViewItem<SpanInsight>))
            }
        }
    }

    result.isOpaque = true
    return result
}


fun spanPanel(listViewItem: ListViewItem<SpanInsight>): JPanel {

    val spanInsight: SpanInsight = listViewItem.modelObject

    val result = panel {
        spanInsight.flows.forEach {
//            if (it.intermediateSpan != null) {
                row {
                    label("a span flow")
                }
//            }
        }
    }

    result.isOpaque = true
    result.background = Color.DARK_GRAY
    return result
}