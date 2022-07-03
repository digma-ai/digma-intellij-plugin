package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.SpanFlow
import org.digma.intellij.plugin.model.rest.insights.SpanInsight
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.common.Html.ARROW_RIGHT
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
        row(asHtml(spanGrayed("Span: ")))
        {
            icon(Icons.Insight.SPAN_GROUP_TITLE).applyToComponent {
                toolTipText = listViewItem.groupId
            }.horizontalAlign(HorizontalAlign.LEFT).gap(RightGap.SMALL)
            cell(CopyableLabel(listViewItem.groupId))
                .applyToComponent {
                    toolTipText = listViewItem.groupId
                }.horizontalAlign(HorizontalAlign.LEFT)

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

    val title = JLabel(asHtml(spanBold("Top Usage")), SwingConstants.LEFT)

    val flowsListPanel = JBPanel<JBPanel<*>>()
    flowsListPanel.layout = GridLayout(spanInsight.flows.size, 1, 0, 3)
    flowsListPanel.border = empty()

    spanInsight.flows.forEach {spanFlow:SpanFlow ->

        val builder =
            StringBuilder("${span(spanFlow.percentage.toString())}% " +
                                "${spanGrayed(spanFlow.firstService?.service.toString())}: " +
                    "           ${span(spanFlow.firstService?.span.toString())}")
        spanFlow.intermediateSpan?.let { intermediateSpan ->
            builder.append(" ${spanGrayed(ARROW_RIGHT)} ")
            builder.append(span(intermediateSpan))
        }
        spanFlow.lastService?.let { lastService ->
            builder.append(" ${spanGrayed(ARROW_RIGHT)} ")
            builder.append(span("${lastService.service}: ${lastService.span}"))
        }
        spanFlow.lastServiceSpan?.let { lastServiceSpan ->
            builder.append(" ${spanGrayed(ARROW_RIGHT)} ")
            builder.append(span(lastServiceSpan))
        }

        val label = CopyableLabelHtml(asHtml(builder.toString()))
        label.alignmentX = 0.0f
        flowsListPanel.add(label)
    }


    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.add(title, BorderLayout.NORTH)
    result.add(flowsListPanel, BorderLayout.CENTER)

    return result

}