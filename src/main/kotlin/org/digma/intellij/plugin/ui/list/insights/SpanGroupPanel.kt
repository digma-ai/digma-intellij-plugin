package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.SpanFlow
import org.digma.intellij.plugin.model.rest.insights.SpanInsight
import org.digma.intellij.plugin.ui.common.Html.ARROW_RIGHT
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.htmlSpanSmoked
import org.digma.intellij.plugin.ui.common.htmlSpanTitle
import org.digma.intellij.plugin.ui.common.htmlSpanWhite
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.GridLayout
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun spanGroupPanel(listViewItem: GroupListViewItem): JPanel {

    val items: SortedSet<ListViewItem<*>> = listViewItem.modelObject

    val result = panel {
        row("Span: ")
        {
            cell(JLabel(asHtml("${htmlSpanTitle()}<b>${listViewItem.groupId}</b>"), Icons.Insight.SPAN_GROUP_TITLE, SwingConstants.LEFT))
                .bold()
        }.topGap(TopGap.SMALL)

        items.forEach {
            row {
                @Suppress("UNCHECKED_CAST")
                cell(insightItemPanel(spanPanel(it as ListViewItem<SpanInsight>)))
                    .horizontalAlign(HorizontalAlign.FILL)
            }.bottomGap(BottomGap.SMALL)
        }
    }

    return insightGroupPanel(result)
}


fun spanPanel(listViewItem: ListViewItem<SpanInsight>): JPanel {

    val spanInsight: SpanInsight = listViewItem.modelObject

    val title = JLabel(asHtml("${htmlSpanTitle()}<b>Top Usage</b>"), SwingConstants.LEFT)


    val flowsListPanel = JBPanel<JBPanel<*>>()
    flowsListPanel.layout = GridLayout(spanInsight.flows.size, 1, 0, 3)
    flowsListPanel.border = empty()

    spanInsight.flows.forEach {spanFlow:SpanFlow ->

        val builder =
            StringBuilder("${htmlSpanWhite()}${spanFlow.percentage}% " +
                                "${htmlSpanSmoked()}${spanFlow.firstService?.service}: " +
                    "           ${htmlSpanWhite()}${spanFlow.firstService?.span}")
        spanFlow.intermediateSpan?.let { intermediateSpan ->
            builder.append(" ${htmlSpanSmoked()}$ARROW_RIGHT ")
            builder.append("${htmlSpanWhite()}$intermediateSpan")
        }
        spanFlow.lastService?.let { lastService ->
            builder.append(" ${htmlSpanSmoked()}$ARROW_RIGHT ")
            builder.append("${htmlSpanWhite()}${lastService.service}: ${lastService.span}")
        }
        spanFlow.lastServiceSpan?.let { lastServiceSpan ->
            builder.append(" ${htmlSpanSmoked()}$ARROW_RIGHT ")
            builder.append("${htmlSpanWhite()}$lastServiceSpan")
        }

        val label = JLabel(asHtml(builder.toString()),SwingConstants.LEFT)
        flowsListPanel.add(label)
    }


    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.add(title, BorderLayout.NORTH)
    result.add(flowsListPanel, BorderLayout.CENTER)

    return result

}