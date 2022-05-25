package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.SpanInsight
import org.digma.intellij.plugin.ui.common.HtmlConsts.ARROW_RIGHT
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.util.*
import javax.swing.*

fun spanGroupPanel(listViewItem: GroupListViewItem): JPanel {

    val items: SortedSet<ListViewItem<*>> = listViewItem.modelObject



    val result = panel {
        row("Span: ")
        {
            cell(JLabel(listViewItem.groupId, Icons.Insight.SPAN_GROUP_TITLE, SwingConstants.LEFT))
        }.bottomGap(BottomGap.SMALL)

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
        row{
            label("Top Usage")
                .bold()
                .verticalAlign(VerticalAlign.TOP)
        }
    }


    val listPanel = JPanel()
    listPanel.layout = BoxLayout(listPanel,BoxLayout.Y_AXIS)

    spanInsight.flows.forEach { spanFlow ->

        val builder = StringBuilder("<html><b>${spanFlow.percentage}%</b>  ${spanFlow.firstService?.service}: <b>${spanFlow.firstService?.span}</b>")
        spanFlow.intermediateSpan?.let { intermediateSpan ->
            builder.append(" $ARROW_RIGHT ")
            builder.append("<b>$intermediateSpan</b>")
        }
        spanFlow.lastService?.let { lastService ->
            builder.append(" $ARROW_RIGHT ")
            builder.append("${lastService.service}: <b>${lastService.span}</b>")
        }
        spanFlow.lastServiceSpan?.let { lastServiceSpan ->
            builder.append(" $ARROW_RIGHT ")
            builder.append("<b>$lastServiceSpan</b>")
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