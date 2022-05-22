package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.SpanInsight
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun spanGroupPanel(listViewItem: GroupListViewItem): JPanel {

    val items: SortedSet<ListViewItem<*>> = listViewItem.modelObject

    val result = panel {
        row(JLabel("  Span: ", Icons.TELESCOPE_12, SwingConstants.LEFT))
        {}.comment(listViewItem.groupId)

        items.forEach {
            row {
                cell(insightItemPanel(spanPanel(it as ListViewItem<SpanInsight>)))
            }
        }
    }

    return insightGroupPanel(result)
}

//working code
//    val result = panel {
//        row(JLabel("  Span: ", Icons.TELESCOPE_12, SwingConstants.LEFT))
//        {}.comment(listViewItem.groupId)
//
//        items.forEach {
//            row {
//                label("Top Usages")
//                    .horizontalAlign(HorizontalAlign.LEFT)
//                    .verticalAlign(VerticalAlign.TOP)
//                    .bold()
//            }
//            indent {
//                row {
//                    cell(spanPanel(it as ListViewItem<SpanInsight>))
//                }
//            }
//        }
//    }
//
//    return result
//}


fun spanPanel(listViewItem: ListViewItem<SpanInsight>): JPanel {

    val spanInsight: SpanInsight = listViewItem.modelObject

    val result = panel {
        spanInsight.flows.forEach { spanFlow ->
            row("Top Usages") {}
            row {
                label(spanFlow.percentage.toString() + "% ${spanFlow.firstService?.service}: ${spanFlow.firstService?.span}")
                    .horizontalAlign(HorizontalAlign.FILL)
//                label(spanFlow.percentage.toString()+"%")
//                    .horizontalAlign(HorizontalAlign.LEFT)
//                label("${spanFlow.firstService?.service}: ")
//                    .horizontalAlign(HorizontalAlign.LEFT)
//                label( "${spanFlow.firstService?.span}")
//                    .horizontalAlign(HorizontalAlign.FILL)
//                    .bold()
            }
//            indent {
//                spanFlow.lastService?.let {lastService ->
//                    row{
//                        label("${lastService.service}: ${lastService.span}")
//                    }
//                }
//                spanFlow.intermediateSpan?.let {intermediateSpan ->
//                    row{
//                        label("$intermediateSpan")
//                    }
//                }
//                spanFlow.lastServiceSpan?.let {lastServiceSpan ->
//                    row{
//                        label("$lastServiceSpan")
//                    }
//                }
//
//            }
        }
    }

    return result
}