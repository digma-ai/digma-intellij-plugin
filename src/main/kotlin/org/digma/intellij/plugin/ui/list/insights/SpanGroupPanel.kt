package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.model.rest.insights.SpanInsight
import org.digma.intellij.plugin.ui.model.listview.Group
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.Color
import javax.swing.JPanel

fun spanGroupPanel(listViewItem: GroupListViewItem): JPanel {

    val group: Group = listViewItem.group

    val result = panel {
        row("Span:"+group.title) {}
        listViewItem.items.forEach {
            row {
                cell(spanPanel(it as ListViewItem<SpanInsight>))
            }
        }
    }

    result.isOpaque = true
    return result
}


fun spanPanel(listViewItem: ListViewItem<SpanInsight>): JPanel {

    val spanInsight: SpanInsight = listViewItem.getModel()

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