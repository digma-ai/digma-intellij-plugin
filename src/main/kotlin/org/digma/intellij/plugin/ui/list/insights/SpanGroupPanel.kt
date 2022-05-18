package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.ui.model.insights.SpanGroupListViewItem
import org.digma.intellij.plugin.ui.model.insights.SpanListViewItem
import java.awt.Color
import javax.swing.JPanel

fun spanGroupPanel(listViewItem: SpanGroupListViewItem): JPanel {

    val result = panel {
        row("Span:"+listViewItem.span) {}
        listViewItem.items.forEach {
            row {
                cell(spanPanel(it as SpanListViewItem))
            }
        }
    }

    result.isOpaque = true
    return result
}


fun spanPanel(spanListViewItem: SpanListViewItem): JPanel {
    val result = panel {
        spanListViewItem.flows.forEach {
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