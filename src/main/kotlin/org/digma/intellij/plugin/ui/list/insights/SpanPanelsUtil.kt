package org.digma.intellij.plugin.ui.list.insights

import com.google.common.io.CharStreams
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.model.rest.insights.DurationSlowdownSource
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import org.digma.intellij.plugin.model.rest.insights.SpanInfo
import org.digma.intellij.plugin.ui.common.CopyableLabelHtml
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.needToShowDurationChange
import org.digma.intellij.plugin.ui.scaled
import org.ocpsoft.prettytime.PrettyTime
import org.threeten.extra.AmountFormats
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.InputStreamReader
import java.sql.Timestamp
import java.time.Duration
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.abs
import kotlin.math.max

typealias DigmaDuration = org.digma.intellij.plugin.model.rest.insights.Duration

const val HTML_NON_BREAKING_SPACE: String = "&nbsp;"

class SpanPanels {

    companion object {
        val JAEGER_EMBEDDED_HTML_TEMPLATE: String

        init {
            this::class.java.getResourceAsStream("/templates/Jaeger-embedded-template.html").use {
                val loadedContent = CharStreams.toString(InputStreamReader(it))
                JAEGER_EMBEDDED_HTML_TEMPLATE = loadedContent
            }
        }
    }
}

fun getLink(project: Project, spanInfo: SpanInfo): ActionLink {

    val spanId = spanInfo.spanCodeObjectId
    val link = ActionLink(spanInfo.name) {
        project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(spanId)
    }
    val targetClass = spanId.substringBeforeLast("\$_\$")

    link.toolTipText = asHtml("$targetClass: $spanInfo.name")
    link.border = empty()
    link.isOpaque = false

    link.minimumSize = link.preferredSize
    return link
}

fun slowdownDurationRowPanel(
    project: Project,
    source: DurationSlowdownSource,
    panelsLayoutHelper: PanelsLayoutHelper,
    gridPanel: JPanel,
    gridLayout: GridBagLayout,
    row: Int
) {
    val c = GridBagConstraints()
    c.fill = GridBagConstraints.HORIZONTAL
    c.weightx = 1.0
    c.gridy = row

    val link = getLink(project, source.spanInfo)
    val linkPanel = JPanel()
    linkPanel.layout = BorderLayout(10.scaled(), 0)
    linkPanel.border = empty()
    linkPanel.isOpaque = false
    linkPanel.add(link, BorderLayout.CENTER)
    gridPanel.add(linkPanel)
    c.gridx = 0
    c.weightx = 1.0
    gridLayout.setConstraints(linkPanel, c)
    c.weightx = 0.0

    //urationsPanel.add(linkPanel, BorderLayout.WEST) // why its not working?

    val pLabelText = spanBold("${source.currentDuration.value} ${source.currentDuration.unit}")
    val durationLabel = CopyableLabelHtml(pLabelText)
    durationLabel.toolTipText = pLabelText
    val durationPanel = object : JPanel() {
        override fun getPreferredSize(): Dimension {
            val ps = super.getPreferredSize()
            if (ps == null) {
                return ps
            }
            val h = ps.height
            val w = ps.width
            addCurrentLargestWidthDurationPLabel(panelsLayoutHelper, w)
            return Dimension(getCurrentLargestWidthDurationPLabel(panelsLayoutHelper, w), h)
        }
    }
    durationPanel.layout = BorderLayout()
    durationPanel.border = empty()
    durationPanel.isOpaque = false
    durationPanel.add(durationLabel, BorderLayout.WEST)
    addCurrentLargestWidthDurationPLabel(panelsLayoutHelper, durationPanel.preferredSize.width)
    gridPanel.add(durationPanel)
    c.gridx = 1
    gridLayout.setConstraints(durationPanel, c)

    val change = createDurationChangeLabel(
        source.currentDuration,
        source.previousDuration,
        source.changeTime)
    gridPanel.add(change)
    c.gridx = 2
    gridLayout.setConstraints(change, c)

    if (source.changeTime != null && (source.changeVerified == null || source.changeVerified == false)) {
        val state = createEvaluationStatePanel(panelsLayoutHelper)
        gridPanel.add(state)
        c.gridx = 3
        gridLayout.setConstraints(change, c)
    }
}

fun percentileRowPanel(
    percentile: SpanDurationsPercentile,
    panelsLayoutHelper: PanelsLayoutHelper,
    traceSamples: ArrayList<TraceSample>,
): JPanel {

    val durationsPanel = JBPanel<JBPanel<*>>()
    durationsPanel.layout = BorderLayout(5, 0)
    durationsPanel.border = empty()
    durationsPanel.isOpaque = false

    val percentileName = "P${(percentile.percentile * 100).toInt()}"
    traceSamples.add(buildTraceSample(percentile))
    val pLabelNumbersText = "${percentile.currentDuration.value} ${percentile.currentDuration.unit}"
    val pLabelText = "$percentileName $HTML_NON_BREAKING_SPACE ${spanBold(pLabelNumbersText)}"
    val pLabel = CopyableLabelHtml(pLabelText)
    pLabel.toolTipText = pLabelText
    val pLabelPanel = object : JPanel() {
        override fun getPreferredSize(): Dimension {
            val ps = super.getPreferredSize()
            if (ps == null) {
                return ps
            }
            val h = ps.height
            val w = ps.width
            addCurrentLargestWidthDurationPLabel(panelsLayoutHelper, w)
            return Dimension(getCurrentLargestWidthDurationPLabel(panelsLayoutHelper, w), h)
        }
    }
    pLabelPanel.layout = BorderLayout()
    pLabelPanel.border = empty()
    pLabelPanel.isOpaque = false
    pLabelPanel.add(pLabel, BorderLayout.WEST)
    addCurrentLargestWidthDurationPLabel(panelsLayoutHelper, pLabelPanel.preferredSize.width)
    durationsPanel.add(pLabelPanel, BorderLayout.WEST)

    if (needToShowDurationChange(percentile)) {
        durationsPanel.add(
            createDurationChangeLabel(
                percentile.currentDuration,
                percentile.previousDuration,
                percentile.changeTime
            ), BorderLayout.CENTER
        )
    }

    if (percentile.changeTime != null && (percentile.changeVerified == null || percentile.changeVerified == false)) {
        durationsPanel.add(createEvaluationStatePanel(panelsLayoutHelper), BorderLayout.EAST)
    }

    return durationsPanel
}

fun createDurationChangeLabel(currentDuration: DigmaDuration, previousDuration: DigmaDuration?, changeTime: Timestamp?)
        : JBLabel {
    val icon =
        if (previousDuration!!.raw > currentDuration.raw) Laf.Icons.Insight.SPAN_DURATION_DROPPED else Laf.Icons.Insight.SPAN_DURATION_ROSE
    val durationText = computeDurationText(currentDuration, previousDuration)
    val whenText = computeWhenText(changeTime)
    val durationLabelText = asHtml(spanGrayed("$durationText,$whenText"))
    val durationLabel = JBLabel(durationLabelText, icon, SwingConstants.LEFT)
    durationLabel.toolTipText = durationLabelText
    return durationLabel
}

fun createEvaluationStatePanel(panelsLayoutHelper: PanelsLayoutHelper): JPanel {
    val evalLabel = JBLabel("Evaluating")
    evalLabel.toolTipText = "This change is still being validated and is based on initial data."
    evalLabel.horizontalAlignment = SwingConstants.RIGHT
    //the evalLabel wants to be aligned with the insights icons panels, so it takes its width from there
    val evalPanel = InsightAlignedPanel(panelsLayoutHelper)
    evalPanel.layout = BorderLayout()
    evalPanel.add(evalLabel, BorderLayout.CENTER)
    evalPanel.isOpaque = false
    addCurrentLargestWidthIconPanel(panelsLayoutHelper, evalPanel.preferredSize.width)
    return evalPanel
}

fun createDefaultBoxLayoutLineAxisPanel(): JPanel {
    return createDefaultBoxLayoutLineAxisPanel(0, 0, 0, 0)
}

fun createDefaultBoxLayoutLineAxisPanel(top: Int, left: Int, bottom: Int, right: Int): JPanel {
    val defaultPanel = JBPanel<JBPanel<*>>()
    defaultPanel.layout = BoxLayout(defaultPanel, BoxLayout.LINE_AXIS)
    defaultPanel.border = empty(top, left, bottom, right)
    defaultPanel.isOpaque = false
    return defaultPanel
}

fun createDefaultBoxLayoutLineAxisPanelWithBackground(color: Color): JPanel {
    return createDefaultBoxLayoutLineAxisPanelWithBackground(0, 0, 0, 0, color)
}

fun createDefaultBoxLayoutLineAxisPanelWithBackground(
    top: Int,
    left: Int,
    bottom: Int,
    right: Int,
    color: Color,
): JPanel {
    val defaultPanel = JBPanel<JBPanel<*>>()
    defaultPanel.layout = BoxLayout(defaultPanel, BoxLayout.LINE_AXIS)
    defaultPanel.border = empty(top, left, bottom, right)
    defaultPanel.background = color
    defaultPanel.isOpaque = true
    return defaultPanel
}

fun createDefaultBoxLayoutLineAxisPanelWithBackgroundWithFixedHeight(
    top: Int,
    left: Int,
    bottom: Int,
    right: Int,
    color: Color,
    height: Int,
): JPanel {
    val defaultPanel = object : JBPanel<JBPanel<*>>() {
        override fun getPreferredSize(): Dimension {
            val size = super.getPreferredSize()
            size.height = height
            return size
        }
    }
    defaultPanel.layout = BoxLayout(defaultPanel, BoxLayout.LINE_AXIS)
    defaultPanel.border = empty(top, left, bottom, right)
    defaultPanel.background = color
    defaultPanel.isOpaque = true
    return defaultPanel
}

fun createDefaultBoxLayoutYAxisPanel(): JPanel {
    val defaultPanel = JBPanel<JBPanel<*>>()
    defaultPanel.layout = BoxLayout(defaultPanel, BoxLayout.Y_AXIS)
    defaultPanel.border = empty()
    defaultPanel.isOpaque = false
    return defaultPanel
}

fun getDefaultSpanOneRecordPanel(): JPanel {
    val spanOneRecordPanel = JPanel(BorderLayout())
    spanOneRecordPanel.border = empty(5, 0)
    spanOneRecordPanel.isOpaque = false
    return spanOneRecordPanel
}

fun buildTraceSample(percentile: SpanDurationsPercentile): TraceSample {
    val percentileName = "P${(percentile.percentile * 100).toInt()}"
    var traceId = ""
    if (!percentile.traceIds.isNullOrEmpty()) {
        traceId = percentile.traceIds!!.first()
    }
    return TraceSample(percentileName, traceId)
}

private fun computeWhenText(changeTime: Timestamp?): String {
    val current = PrettyTime(Timestamp(System.currentTimeMillis()))
    return current.format(current.approximateDuration(changeTime))
}

private fun computeDurationText(currentDuration: DigmaDuration, previousDuration: DigmaDuration?): String {

    val durationMillis =
        TimeUnit.MILLISECONDS.convert(
            abs(previousDuration!!.raw - currentDuration.raw),
            TimeUnit.NANOSECONDS
        )
    val javaDuration = Duration.ofMillis(durationMillis)
    if (javaDuration.isZero) {
        return "a few milliseconds"
    }
    return AmountFormats.wordBased(javaDuration, Locale.getDefault())
//kotlin
//java.time.Duration.ofMillis(80000).abs().toString().substring(2).replace("(\\d[HMS])(?!$)".toRegex(),"$1 ").lowercase()
//1m 20s

}


fun getCurrentLargestWidthDurationPLabel(layoutHelper: PanelsLayoutHelper, width: Int): Int {
    //this method should never return null and never throw NPE
    val currentLargest: Int =
        (layoutHelper.getObjectAttribute("SpanDurationsDurationPLabel", "largestWidth") ?: 0) as Int
    return max(width, currentLargest)
}

fun addCurrentLargestWidthDurationPLabel(layoutHelper: PanelsLayoutHelper, width: Int) {
    //this method should never throw NPE
    val currentLargest: Int =
        (layoutHelper.getObjectAttribute("SpanDurationsDurationPLabel", "largestWidth") ?: 0) as Int
    layoutHelper.addObjectAttribute(
        "SpanDurationsDurationPLabel", "largestWidth",
        max(currentLargest, width)
    )
}