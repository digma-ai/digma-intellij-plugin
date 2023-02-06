package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Borders.empty
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.model.rest.insights.SpanDurationBreakdown
import org.digma.intellij.plugin.model.rest.insights.SpanDurationBreakdownInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.boldFonts
import org.digma.intellij.plugin.ui.list.openWorkspaceFileForSpan
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
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
        moreData: HashMap<String, Any>
): JPanel {

    resultBreakdownPanel = object : DigmaResettablePanel() {
        override fun reset() {
            buildDurationBreakdownRowPanel(
                    resultBreakdownPanel!!,
                    durationBreakdownEntriesToDisplay,
                    project,
                    moreData
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
        moreData: HashMap<String, Any>
) {
    durationBreakdownPanel.removeAll()
    resultBreakdownPanel!!.layout = BoxLayout(durationBreakdownPanel, BoxLayout.Y_AXIS)
    resultBreakdownPanel!!.isOpaque = false

    durationBreakdownEntries.forEach { durationBreakdown: SpanDurationBreakdown ->
        resultBreakdownPanel!!.add(durationBreakdownRowPanel(durationBreakdown, project, moreData))
    }
}

fun durationBreakdownRowPanel(
        durationBreakdown: SpanDurationBreakdown,
        project: Project,
        moreData: HashMap<String, Any>
): JPanel {
    val durationBreakdownPanel = getDurationBreakdownPanel()
    val telescopeIconLabel = getTelescopeIconLabel()
    val spanDisplayNameLabel = getSpanDisplayNameLabel(durationBreakdown, project, moreData)
    val breakdownDurationLabelPanel = getBreakdownDurationLabel(durationBreakdown)

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
    paginationLabel.border = JBUI.Borders.emptyLeft(5)

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
        moreData: HashMap<String, Any>,
): JComponent {
    val spanId = CodeObjectsUtil.createSpanId(durationBreakdown.spanInstrumentationLibrary, durationBreakdown.spanName)
    val trimmedDisplayName = StringUtils.normalizeSpace(durationBreakdown.spanDisplayName)

    val messageLabel = if (moreData.contains(spanId)) {
        ActionLink(trimmedDisplayName) {
            openWorkspaceFileForSpan(project, moreData, spanId)
        }
    } else {
        JLabel(trimmedDisplayName)
    }
    messageLabel.toolTipText = asHtml(trimmedDisplayName)
    messageLabel.border = empty(0, 5, 5, 0)
    messageLabel.isOpaque = false

    messageLabel.minimumSize = messageLabel.preferredSize

    return messageLabel
}

private fun getBreakdownDurationLabel(
        durationBreakdown: SpanDurationBreakdown
): JComponent {
    val pLabelText = getDisplayValueOfPercentile(durationBreakdown, P_50)
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
