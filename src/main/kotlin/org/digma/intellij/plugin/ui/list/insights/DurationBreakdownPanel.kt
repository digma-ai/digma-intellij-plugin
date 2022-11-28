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
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.*

private const val P_50: Float = 0.5F
private const val RECORDS_PER_PAGE = 3

private var lastPageNum = 0
private var currPageNum = 0
private var prev: ActionLink? = null
private var next: ActionLink? = null
private var resultBreakdownPanel: DigmaResettablePanel? = null
private val paginationPanel = JPanel()
val durationBreakdownEntriesToDisplay = ArrayList<SpanDurationBreakdown>()

fun spanDurationBreakdownPanel(
        project: Project,
        insight: SpanDurationBreakdownInsight,
        moreData: HashMap<String, Any>,
        panelsLayoutHelper: PanelsLayoutHelper
): JPanel {

    resultBreakdownPanel = object : DigmaResettablePanel() {
        override fun reset() {
            buildDurationBreakdownRowPanel(
                    resultBreakdownPanel!!,
                    durationBreakdownEntriesToDisplay,
                    project,
                    moreData,
                    panelsLayoutHelper
            )
            buildPaginationPanel(paginationPanel)
        }
    }

    val validBreakdownEntries = insight.breakdownEntries
            .filter { entry -> entry.percentiles.any { breakdown -> breakdown.percentile.equals(P_50) } }
            .sortedWith(compareByDescending { getValueOfPercentile(it, P_50) })

    //calculate how many pages there are
    lastPageNum = validBreakdownEntries.size / RECORDS_PER_PAGE + if (validBreakdownEntries.size % RECORDS_PER_PAGE != 0) 1 else 0
    currPageNum = if (lastPageNum > 0) 1 else 0

    updateDurationBreakdownPanel(validBreakdownEntries)
    return createInsightPanel(
            project = project,
            insight = insight,
            title = "Duration Breakdown",
            description = "",
            iconsList = listOf(Laf.Icons.Insight.DURATION),
            bodyPanel = resultBreakdownPanel,
            buttons = null,
            paginationComponent = paginationRowPanel(validBreakdownEntries),
    )
}

fun buildDurationBreakdownRowPanel(
        durationBreakdownPanel: DigmaResettablePanel,
        durationBreakdownEntries: List<SpanDurationBreakdown>,
        project: Project,
        moreData: HashMap<String, Any>,
        layoutHelper: PanelsLayoutHelper
) {
    durationBreakdownPanel.removeAll()
    resultBreakdownPanel!!.layout = GridLayout(durationBreakdownEntries.size, 1, 0, 2)
    resultBreakdownPanel!!.isOpaque = false

    durationBreakdownEntries.forEach { durationBreakdown: SpanDurationBreakdown ->
        resultBreakdownPanel!!.add(durationBreakdownRowPanel(durationBreakdown, project, moreData, layoutHelper))
    }
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

fun paginationRowPanel(
        durationBreakdownEntries: List<SpanDurationBreakdown>
): JPanel? {
    if (lastPageNum < 2) {
        return null
    }
    prev = ActionLink("Prev")
    prev!!.addActionListener {
        if (--currPageNum <= 0) currPageNum = 1
        updateDurationBreakdownPanel(durationBreakdownEntries)
    }
    next = ActionLink("Next")
    next!!.addActionListener {
        if (++currPageNum > lastPageNum) currPageNum = lastPageNum
        updateDurationBreakdownPanel(durationBreakdownEntries)
    }
    buildPaginationPanel(paginationPanel)
    return paginationPanel
}

fun buildPaginationPanel(paginationPanel: JPanel) {
    paginationPanel.removeAll()

    paginationPanel.layout = BorderLayout()
    paginationPanel.border = empty()
    paginationPanel.isOpaque = false

    val paginationLabelText = "$currPageNum of $lastPageNum"
    val paginationLabel = JLabel(asHtml(paginationLabelText), SwingConstants.LEFT)
    paginationLabel.border = empty(0, 5, 0, 0)

    prev?.let { paginationPanel.add(it, BorderLayout.WEST) }
    next?.let { paginationPanel.add(it, BorderLayout.CENTER) }
    paginationPanel.add(paginationLabel, BorderLayout.EAST)

    val canGoBack = currPageNum > 1
    val canGoFwd = currPageNum != lastPageNum
    prev?.let { it.isEnabled = canGoBack }
    next?.let { it.isEnabled = canGoFwd }
}


private fun updateDurationBreakdownPanel(durationBreakdownEntries: List<SpanDurationBreakdown>) {
    durationBreakdownEntriesToDisplay.clear()

    if (durationBreakdownEntries.isNotEmpty()) {
        val start = (currPageNum - 1) * RECORDS_PER_PAGE
        var end = start + RECORDS_PER_PAGE
        if (end >= durationBreakdownEntries.size) {
            end = durationBreakdownEntries.size
        }
        for (i in start until end) {
            durationBreakdownEntriesToDisplay.add(durationBreakdownEntries[i])
        }
    }

    resultBreakdownPanel!!.reset()
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

    val breakdownDurationLabelPanel = JPanel()
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
    for (p in sortedPercentiles) {
        tooltip += "P${(p.percentile * 100).toInt()}: ${p.duration.value} ${p.duration.unit}".plus("<br>")
    }
    return asHtml(tooltip)
}