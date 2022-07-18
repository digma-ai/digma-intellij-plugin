package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import org.digma.intellij.plugin.model.rest.insights.SpanFlow
import org.digma.intellij.plugin.model.rest.insights.SpanInsight
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.common.Html.ARROW_RIGHT
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.listview.GroupListViewItem
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import org.ocpsoft.prettytime.PrettyTime
import org.threeten.extra.AmountFormats
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.abs
import kotlin.math.max


fun spanGroupPanel(listViewItem: GroupListViewItem, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

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
                val modelObject = it.modelObject
                val cellItem =
                    when (modelObject) {
                        is SpanInsight -> spanPanel(modelObject)
                        is SpanDurationsInsight -> spanDurationPanel(modelObject,panelsLayoutHelper)
                        else -> panelOfUnsupported("${modelObject?.javaClass?.simpleName}")
                    }

                cell(insightItemPanel(cellItem))
                    .horizontalAlign(HorizontalAlign.FILL)
            }.bottomGap(BottomGap.SMALL)
        }
    }

    return insightGroupPanel(result)
}


fun spanPanel(spanInsight: SpanInsight): JPanel {

    val title = JLabel(asHtml(spanBold("Top Usage")), SwingConstants.LEFT)
    title.isOpaque = false

    val flowsListPanel = JBPanel<JBPanel<*>>()
    flowsListPanel.layout = GridLayout(spanInsight.flows.size, 1, 0, 3)
    flowsListPanel.border = empty()
    flowsListPanel.isOpaque = false

    spanInsight.flows.forEach { spanFlow: SpanFlow ->

        val builder =
            StringBuilder("${span(String.format("%.1f", spanFlow.percentage))}% " +
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


fun spanDurationPanel(spanDurationsInsight: SpanDurationsInsight, panelsLayoutHelper: PanelsLayoutHelper): JPanel {

    if (spanDurationsInsight.percentiles.isEmpty()) {
        return createInsightPanel("Duration", "Waiting for more data.", Icons.Insight.WAITING_DATA, "", false,panelsLayoutHelper)
    }

    val title = JLabel(asHtml(spanBold("Duration")), SwingConstants.LEFT)

    val durationsListPanel = JBPanel<JBPanel<*>>()
    durationsListPanel.layout = GridLayout(spanDurationsInsight.percentiles.size, 1, 0, 2)

    val sortedPercentiles = spanDurationsInsight.percentiles.sortedBy {
        it.percentile
    }

    val tolerationConstant: Long = 10000

    sortedPercentiles.forEach { percentile: SpanDurationsPercentile ->

        var changeMeaningfulEnough = false
        val durationsPanel = JBPanel<JBPanel<*>>()
        durationsPanel.layout = BorderLayout(5, 0)
        durationsPanel.border = empty()
        durationsPanel.isOpaque = false

        val pLabelText = "P${percentile.percentile * 100} ${percentile.currentDuration.value} ${percentile.currentDuration.unit}"
        val pLabel = CopyableLabel(pLabelText)
        pLabel.toolTipText = pLabelText
        val pLabelPanel = object : JPanel() {
            override fun getPreferredSize(): Dimension {
                val ps = super.getPreferredSize()
                if (ps == null) {
                    return ps
                }
                val h = ps.height
                val w = ps.width
                addCurrentLargestWidthDurationPLabel(panelsLayoutHelper,w)
                return Dimension(getCurrentLargestWidthDurationPLabel(panelsLayoutHelper,w), h)
            }
        }
        pLabelPanel.layout = BorderLayout()
        pLabelPanel.border = empty()
        pLabelPanel.isOpaque = false
        pLabelPanel.add(pLabel, BorderLayout.WEST)
        addCurrentLargestWidthDurationPLabel(panelsLayoutHelper,pLabelPanel.preferredSize.width)
        durationsPanel.add(pLabelPanel, BorderLayout.WEST)


        if (percentile.previousDuration != null && percentile.changeTime != null) {

            val rawDiff: Long = abs(percentile.currentDuration.raw - percentile.previousDuration!!.raw)
            changeMeaningfulEnough = ((rawDiff / percentile.previousDuration!!.raw) > 0.1) && (rawDiff > tolerationConstant)
            if (changeMeaningfulEnough) {
                val icon = if (percentile.previousDuration!!.raw > percentile.currentDuration.raw) Icons.Insight.SPAN_DURATION_DROPPED else Icons.Insight.SPAN_DURATION_ROSE
                val durationText = computeDurationText(percentile)
                val whenText = computeWhenText(percentile)
                val durationLabelText = asHtml(spanGrayed("$durationText,$whenText"))
                val durationLabel = JBLabel(durationLabelText, icon, SwingConstants.LEFT)
                durationLabel.toolTipText = durationLabelText
                durationsPanel.add(durationLabel, BorderLayout.CENTER)
            }
        }

        if (percentile.changeTime != null && (percentile.changeVerified == null || percentile.changeVerified == false)) {

            val evalLabel = JBLabel("Evaluating")
            evalLabel.toolTipText = "This change is still being validated and is based on initial data."
            evalLabel.horizontalAlignment = SwingConstants.RIGHT
            //the evalLabel wants to be aligned with the insights icons panels, so it takes its width from there
            val evalPanel = object : JPanel() {
                override fun getPreferredSize(): Dimension {
                    val ps = super.getPreferredSize()
                    if (ps == null) {
                        return ps
                    }
                    val h = ps.height
                    val w = ps.width
                    addCurrentLargestWidthIconPanel(panelsLayoutHelper,w)
                    return Dimension(getCurrentLargestWidthIconPanel(panelsLayoutHelper,w), h)
                }
            }
            evalPanel.layout = BorderLayout()
            evalPanel.add(evalLabel, BorderLayout.CENTER)
            evalPanel.border = empty(0, 0, 0, Laf.scaleBorders(getInsightIconPanelRightBorderSize()))
            addCurrentLargestWidthIconPanel(panelsLayoutHelper,evalPanel.preferredSize.width)
            durationsPanel.add(evalPanel, BorderLayout.EAST)
        }

        durationsListPanel.add(durationsPanel)

    }


    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.isOpaque = false
    result.add(title, BorderLayout.NORTH)
    result.add(durationsListPanel, BorderLayout.CENTER)
    return result
}

private fun computeWhenText(percentile: SpanDurationsPercentile): String {
    val current = PrettyTime(Timestamp(System.currentTimeMillis()))
    return current.format(current.calculatePreciseDuration(percentile.changeTime))
}

private fun computeDurationText(percentile: SpanDurationsPercentile): String {

    val durationMillis =
        TimeUnit.MILLISECONDS.convert(abs(percentile.previousDuration!!.raw - percentile.currentDuration.raw),
            TimeUnit.NANOSECONDS)
    val javaDuration = java.time.Duration.ofMillis(durationMillis)
    if (javaDuration.isZero) {
        return "a few milliseconds"
    }
    return AmountFormats.wordBased(javaDuration, Locale.getDefault())
//kotlin
//java.time.Duration.ofMillis(80000).abs().toString().substring(2).replace("(\\d[HMS])(?!$)".toRegex(),"$1 ").lowercase()
//1m 20s

}


private fun getCurrentLargestWidthDurationPLabel(layoutHelper: PanelsLayoutHelper, width: Int): Int {
    //this method should never return null and never throw NPE
    val currentLargest: Int =
        (layoutHelper.getObjectAttribute("SpanDurationsDurationPLabel", "largestWidth") ?: 0) as Int
    return max(width, currentLargest)
}

private fun addCurrentLargestWidthDurationPLabel(layoutHelper: PanelsLayoutHelper,width: Int) {
    //this method should never throw NPE
    val currentLargest: Int =
        (layoutHelper.getObjectAttribute("SpanDurationsDurationPLabel", "largestWidth") ?: 0) as Int
    layoutHelper.addObjectAttribute("SpanDurationsDurationPLabel", "largestWidth",
        max(currentLargest, width))
}