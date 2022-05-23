package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.SpanFlow
import org.digma.intellij.plugin.model.rest.insights.SpanInsight
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.util.*
import javax.swing.*

fun spanGroupPanel(listViewItem: GroupListViewItem): JPanel {

    val items: SortedSet<ListViewItem<*>> = listViewItem.modelObject

    val result = panel {
        row(JLabel("  Span: ", Icons.TELESCOPE_12, SwingConstants.LEFT))
        {}.bottomGap(BottomGap.SMALL)
            .topGap(TopGap.SMALL)
            .comment(listViewItem.groupId)

        items.forEach {
            row {
                cell(insightItemPanel(spanPanel(it as ListViewItem<SpanInsight>)))
                    .horizontalAlign(HorizontalAlign.FILL)
            }
        }
    }

    return insightGroupPanel(result)
}




fun spanPanel(listViewItem: ListViewItem<SpanInsight>): JPanel {

    val spanInsight: SpanInsight = listViewItem.modelObject
    //temp
//    val spanInsightOrg: SpanInsight = listViewItem.modelObject
//    val list = ArrayList<SpanFlow>()
//    if (spanInsightOrg.flows != null && spanInsightOrg.flows.isNotEmpty()) {
//        list.add(spanInsightOrg.flows.first())
//    }
//    list.add(SpanFlow(80f,
//        "intermediateSpan1",
//        "lastservicespan1",
//        SpanFlow.Service("service1", listViewItem.modelObject.span),
//        SpanFlow.Service("lastservice1", listViewItem.modelObject.span)))
//    list.add(SpanFlow(100f,
//        "intermediateSpan2",
//        "lastservicespan2",
//        SpanFlow.Service("service2", listViewItem.modelObject.span),
//        SpanFlow.Service("lastservice2", listViewItem.modelObject.span)))
//    val spanInsight = SpanInsight(listViewItem.modelObject.codeObjectId, listViewItem.modelObject.span, list)


    val title = panel{
        row{}.comment("Top Usages").bold()
    }


    val listPanel = JPanel()
    listPanel.layout = BoxLayout(listPanel,BoxLayout.Y_AXIS)

    spanInsight.flows.forEach { spanFlow ->

        val builder = StringBuilder("<html><b>${spanFlow.percentage}%  ${spanFlow.firstService?.service}: ${spanFlow.firstService?.span}</b>")
        spanFlow.intermediateSpan?.let { intermediateSpan ->
            builder.append(" &#8594; ")
            builder.append("$intermediateSpan")
        }
        spanFlow.lastService?.let { lastService ->
            builder.append(" &#8594; ")
            builder.append("${lastService.service}: ${lastService.span}")
        }
        spanFlow.lastServiceSpan?.let { lastServiceSpan ->
            builder.append(" &#8594; ")
            builder.append("$lastServiceSpan")
        }

        val label = JLabel(builder.toString())
        label.horizontalAlignment = SwingConstants.LEFT
        listPanel.add(label)
        listPanel.add(Box.createVerticalStrut(5))
    }


    val result = JPanel()
    result.layout = BorderLayout()
    result.add(title,BorderLayout.NORTH)
    result.add(listPanel, BorderLayout.CENTER)

    return result

}

//fun spanPanel(listViewItem: ListViewItem<SpanInsight>): JPanel {
//
//    //working example with labels
//
//    val result = panel {
//
//        val spanInsight: SpanInsight = listViewItem.modelObject
//
//        row {}.comment("Top Usages")
//
//        spanInsight.flows.forEach { spanFlow ->
//            row {
//                label(spanFlow.percentage.toString() + "% ${spanFlow.firstService?.service}: ${spanFlow.firstService?.span}")
//                    .horizontalAlign(HorizontalAlign.LEFT)
//                spanFlow.intermediateSpan?.let { intermediateSpan ->
//                    icon(AllIcons.Icons.Ide.MenuArrowSelected)
//                    label("$intermediateSpan")
//                }
//                spanFlow.lastService?.let { lastService ->
//                    icon(AllIcons.Icons.Ide.MenuArrowSelected)
//                    label("${lastService.service}: ${lastService.span}")
//                }
//                spanFlow.lastServiceSpan?.let { lastServiceSpan ->
//                    icon(AllIcons.Icons.Ide.MenuArrowSelected)
//                    label("$lastServiceSpan")
//                }
//            }
//        }
//    }
//
//    return result
//}