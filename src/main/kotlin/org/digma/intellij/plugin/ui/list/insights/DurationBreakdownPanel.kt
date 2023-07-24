package org.digma.intellij.plugin.ui.list.insights

import LabeledSwitch
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders.empty
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.editor.getCurrentPageNumberForInsight
import org.digma.intellij.plugin.editor.updateListOfEntriesToDisplay
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.SpanDurationBreakdown
import org.digma.intellij.plugin.model.rest.insights.SpanDurationBreakdownInsight
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.boldFonts
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

private const val P_95: Float = 0.95F
private const val P_50: Float = 0.5F
private const val RECORDS_PER_PAGE_DURATION_BREAKDOWN = 3

fun spanDurationBreakdownPanel(
    project: Project,
    insight: SpanDurationBreakdownInsight,
): JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type
    val lastPageNum: Int
    var activePercentile: Float = P_50;
    var resultBreakdownPanel: DigmaResettablePanel? = null
    val paginationPanel = JPanel()
    val durationBreakdownEntriesToDisplay = ArrayList<SpanDurationBreakdown>()

    var filteredBreakdownEntities = ArrayList<SpanDurationBreakdown>()

    filterDurationBreakdownData(insight, filteredBreakdownEntities, activePercentile)

    //calculate how many pages there are
    lastPageNum =
        filteredBreakdownEntities.size / RECORDS_PER_PAGE_DURATION_BREAKDOWN + if (filteredBreakdownEntities.size % RECORDS_PER_PAGE_DURATION_BREAKDOWN != 0) 1 else 0

    val switchLabelPanel = JBPanel<JBPanel<*>>()
    switchLabelPanel.layout = BorderLayout(5, 5)
    switchLabelPanel.border = empty(5, 0, 5, 0)
    switchLabelPanel.isOpaque = false

    val labeledSwitch = LabeledSwitch("Median", "Slowest 5%") { isOn ->
        var percentile = if (isOn) P_50 else P_95
        activePercentile = percentile;
        filterDurationBreakdownData(insight, filteredBreakdownEntities, percentile)
        resultBreakdownPanel!!.reset();
    }

    switchLabelPanel.add(labeledSwitch, BorderLayout.WEST)

    resultBreakdownPanel = object : DigmaResettablePanel() {
        override fun reset() {
            updateListOfEntriesToDisplay(
                filteredBreakdownEntities,
                durationBreakdownEntriesToDisplay,
                getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum),
                RECORDS_PER_PAGE_DURATION_BREAKDOWN,
                project
            )
            rebuildDurationBreakdownRowPanel(
                resultBreakdownPanel!!,
                durationBreakdownEntriesToDisplay,
                switchLabelPanel,
                activePercentile,
                project
            )
            rebuildPaginationPanel(
                paginationPanel,
                lastPageNum,
                filteredBreakdownEntities,
                resultBreakdownPanel,
                durationBreakdownEntriesToDisplay,
                uniqueInsightId,
                RECORDS_PER_PAGE_DURATION_BREAKDOWN,
                project,
                insight.type
            )
        }
    }
    resultBreakdownPanel.add(switchLabelPanel);

    updateListOfEntriesToDisplay(
        filteredBreakdownEntities,
        durationBreakdownEntriesToDisplay,
        getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum),
        RECORDS_PER_PAGE_DURATION_BREAKDOWN,
        project
    )
    buildDurationBreakdownRowPanel(resultBreakdownPanel, durationBreakdownEntriesToDisplay, activePercentile, project)

    return createInsightPanel(
        project = project,
        insight = insight,
        title = "Duration Breakdown",
        description = "",
        iconsList = listOf(Laf.Icons.Insight.DURATION),
        bodyPanel = resultBreakdownPanel,
        buttons = null,
        paginationComponent = buildPaginationRowPanel(
            lastPageNum,
            paginationPanel,
            filteredBreakdownEntities,
            resultBreakdownPanel,
            durationBreakdownEntriesToDisplay,
            uniqueInsightId,
            RECORDS_PER_PAGE_DURATION_BREAKDOWN,
            project,
            insight.type
        ),
    )
}

private fun filterDurationBreakdownData(
    insight: SpanDurationBreakdownInsight,
    filteredBreakdownEntities: ArrayList<SpanDurationBreakdown>,
    percentile: Float){
    filteredBreakdownEntities.clear();
    insight.breakdownEntries.sortedWith(compareByDescending { getValueOfPercentile(it, percentile) }).forEach { item ->
        var percentiles = item.percentiles.filter { p -> p.percentile.equals(percentile) };
        var clone = SpanDurationBreakdown(
            item.spanName, item.spanDisplayName, item.spanInstrumentationLibrary, item.spanCodeObjectId,
            percentiles
        );
        filteredBreakdownEntities.add(clone);
    }
}

private fun buildDurationBreakdownRowPanel(
    durationBreakdownPanel: DigmaResettablePanel,
    durationBreakdownEntriesToDisplay: List<SpanDurationBreakdown>,
    percentile: Float,
    project: Project,
) {
    durationBreakdownPanel.layout = BoxLayout(durationBreakdownPanel, BoxLayout.Y_AXIS)
    durationBreakdownPanel.isOpaque = false

    durationBreakdownEntriesToDisplay.forEach { durationBreakdown: SpanDurationBreakdown ->
        durationBreakdownPanel.add(durationBreakdownRowPanel(durationBreakdown, project, percentile))
    }
}

private fun rebuildDurationBreakdownRowPanel(
    durationBreakdownPanel: DigmaResettablePanel,
    durationBreakdownEntriesToDisplay: List<SpanDurationBreakdown>,
    switchPanel: JPanel,
    percentile: Float,
    project: Project,
) {
    durationBreakdownPanel.removeAll()
    durationBreakdownPanel.add(switchPanel);
    buildDurationBreakdownRowPanel(durationBreakdownPanel, durationBreakdownEntriesToDisplay, percentile, project)
}

private fun durationBreakdownRowPanel(
    durationBreakdown: SpanDurationBreakdown,
    project: Project,
    percentile: Float
): JPanel {
    val durationBreakdownPanel = getDurationBreakdownPanel()
    val telescopeIconLabel = getTelescopeIconLabel()
    val spanDisplayNameLabel = getSpanDisplayNameLabel(durationBreakdown, project)
    val breakdownDurationLabelPanel = getBreakdownDurationLabel(durationBreakdown, percentile)

    durationBreakdownPanel.add(telescopeIconLabel, BorderLayout.WEST)
    durationBreakdownPanel.add(spanDisplayNameLabel, BorderLayout.CENTER)
    durationBreakdownPanel.add(breakdownDurationLabelPanel, BorderLayout.EAST)

    return durationBreakdownPanel
}

private fun getDurationBreakdownPanel(): JPanel {
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
): JComponent {
    val spanId = durationBreakdown.spanCodeObjectId
    val trimmedDisplayName = StringUtils.normalizeSpace(durationBreakdown.spanDisplayName)

    val messageLabel = ActionLink(trimmedDisplayName) {
        ActivityMonitor.getInstance(project).registerSpanLinkClicked(InsightType.SpanDurationBreakdown)
        project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(spanId)
    }
    messageLabel.toolTipText = asHtml(trimmedDisplayName)
    messageLabel.border = empty(0, 5, 5, 0)
    messageLabel.isOpaque = false

    messageLabel.minimumSize = messageLabel.preferredSize

    return messageLabel
}

private fun getBreakdownDurationLabel(
    durationBreakdown: SpanDurationBreakdown,
    percentile: Float
): JComponent {
    val pLabelText = getDisplayValueOfPercentile(durationBreakdown, percentile)
    val pLabel = JLabel(pLabelText)
    pLabel.toolTipText = getTooltipForDurationLabel(durationBreakdown)
    boldFonts(pLabel)

    return pLabel
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
    for (p in sortedPercentiles) {
        tooltip += "P${(p.percentile * 100).toInt()}: ${p.duration.value} ${p.duration.unit}".plus("<br>")
    }
    return asHtml(tooltip)
}

