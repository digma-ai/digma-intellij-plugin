package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.model.rest.insights.SpanDurationBreakdown
import org.digma.intellij.plugin.model.rest.insights.SpanDurationBreakdownInsight
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

const val P_50: Float = 0.5F

fun spanDurationBreakdownPanel(
        project: Project,
        insight: SpanDurationBreakdownInsight,
        moreData: HashMap<String, Any>,
        panelsLayoutHelper: PanelsLayoutHelper
): JPanel {

    val resultBreakdownPanel = JBPanel<JBPanel<*>>()
    resultBreakdownPanel.layout = GridLayout(insight.breakdownEntries.size, 1, 0, 2)
    resultBreakdownPanel.isOpaque = false

    insight.breakdownEntries
            .filter { entry -> entry.percentiles.any { breakdown -> breakdown.percentile.equals(P_50) } }
            .sortedWith(compareByDescending { getValueOfPercentile(it, P_50) })
            .forEach { durationBreakdown: SpanDurationBreakdown ->
                val durationBreakdownPanel = durationBreakdownRowPanel(durationBreakdown, project, moreData, panelsLayoutHelper)
                resultBreakdownPanel.add(durationBreakdownPanel)
            }

    return createInsightPanel(
            "Duration Breakdown",
            "",
            Laf.Icons.Insight.DURATION,
            resultBreakdownPanel,
            null,
            panelsLayoutHelper)
}

fun durationBreakdownRowPanel(
        durationBreakdown: SpanDurationBreakdown,
        project: Project,
        moreData: HashMap<String, Any>,
        layoutHelper: PanelsLayoutHelper
): JPanel {
    val durationBreakdownPanel = getDurationBreakdownPanel()
    val telescopeIconLabel = getTelescopeIconLabel()
    val spanDisplayNameLabel = getSpanDisplayNameLabel(durationBreakdown, project, moreData)
    val breakdownDurationLabelPanel = getBreakdownDurationLabelPanel(durationBreakdown, layoutHelper)

    durationBreakdownPanel.add(telescopeIconLabel, BorderLayout.WEST)
    durationBreakdownPanel.add(spanDisplayNameLabel, BorderLayout.CENTER)
    durationBreakdownPanel.add(breakdownDurationLabelPanel, BorderLayout.EAST)

    return durationBreakdownPanel
}

private fun getDurationBreakdownPanel(): JBPanel<JBPanel<*>> {
    val durationBreakdownPanel = JBPanel<JBPanel<*>>()
    durationBreakdownPanel.layout = BorderLayout(5, 0)
    durationBreakdownPanel.border = empty()
    durationBreakdownPanel.isOpaque = false
    return durationBreakdownPanel
}

private fun getTelescopeIconLabel(): JLabel {
    val iconLabel = JLabel(Laf.Icons.Insight.TELESCOPE, SwingConstants.LEFT)
    iconLabel.horizontalAlignment = SwingConstants.LEFT
    iconLabel.verticalAlignment = SwingConstants.TOP
    iconLabel.isOpaque = false
    return iconLabel
}

private fun getSpanDisplayNameLabel(
        durationBreakdown: SpanDurationBreakdown,
        project: Project,
        moreData: HashMap<String, Any>,
): JComponent {
    val spanId = CodeObjectsUtil.createSpanId(durationBreakdown.spanInstrumentationLibrary, durationBreakdown.spanName)
    val displayName = durationBreakdown.spanDisplayName

    val messageLabel = if (moreData.contains(spanId)) {
        ActionLink(asHtml(displayName)) {
            val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)

            @Suppress("UNCHECKED_CAST")
            val workspaceUri: Pair<String, Int> = moreData[spanId] as Pair<String, Int>
            actionListener.openWorkspaceFileForSpan(workspaceUri.first, workspaceUri.second)
        }
    } else {
        JLabel(asHtml(displayName), SwingConstants.LEFT)
    }
    messageLabel.toolTipText = displayName
    messageLabel.border = empty(0, 0, 5, 0)
    messageLabel.isOpaque = false

    return messageLabel
}

private fun getBreakdownDurationLabelPanel(
        durationBreakdown: SpanDurationBreakdown,
        layoutHelper: PanelsLayoutHelper
): JPanel {
    val pLabelText = getDisplayValueOfPercentile(durationBreakdown, P_50)
    val pLabel = JLabel(pLabelText)
    pLabel.toolTipText = getTooltipForDurationLabel(durationBreakdown)
    boldFonts(pLabel)

    val breakdownDurationLabelPanel = object : JPanel() {
        override fun getPreferredSize(): Dimension {
            val ps = super.getPreferredSize()
            if (ps == null) {
                return ps
            }
            val h = ps.height
            val w = ps.width
            addCurrentLargestWidthDurationPLabel(layoutHelper, w)
            return Dimension(getCurrentLargestWidthDurationPLabel(layoutHelper, w), h)
        }
    }
    breakdownDurationLabelPanel.layout = BorderLayout()
    breakdownDurationLabelPanel.border = empty()
    breakdownDurationLabelPanel.isOpaque = false
    breakdownDurationLabelPanel.add(pLabel, BorderLayout.WEST)
    addCurrentLargestWidthDurationPLabel(layoutHelper, breakdownDurationLabelPanel.preferredSize.width)

    return breakdownDurationLabelPanel
}

private fun getDisplayValueOfPercentile(breakdownEntry: SpanDurationBreakdown, requestedPercentile: Float): String {
    for (pctl in breakdownEntry.percentiles) {
        if (pctl.percentile.equals(requestedPercentile)) {
            return "${pctl.duration.value} ${pctl.duration.unit}"
        }
    }
    return ""
}

private fun getValueOfPercentile(breakdownEntry: SpanDurationBreakdown, requestedPercentile: Float): Long? {
    for (pctl in breakdownEntry.percentiles) {
        if (pctl.percentile.equals(requestedPercentile)) {
            return pctl.duration.raw
        }
    }
    return null
}

private fun getTooltipForDurationLabel(breakdownEntry: SpanDurationBreakdown): String {
    val sortedPercentiles = breakdownEntry.percentiles.sortedBy { it.percentile }
    var tooltip = "Percentage of time spent in span:".plus("<br>")
    for(p in sortedPercentiles){
        tooltip += "P${(p.percentile*100).toInt()}: ${p.duration.value} ${p.duration.unit}".plus("<br>")
    }
    return asHtml(tooltip)
}