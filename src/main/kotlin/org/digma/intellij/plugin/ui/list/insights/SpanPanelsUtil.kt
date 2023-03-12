package org.digma.intellij.plugin.ui.list.insights

import com.google.common.io.CharStreams
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.needToShowDurationChange
import org.ocpsoft.prettytime.PrettyTime
import org.threeten.extra.AmountFormats
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.InputStreamReader
import java.sql.Timestamp
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.abs
import kotlin.math.max

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

fun percentileRowPanel(percentile: SpanDurationsPercentile, panelsLayoutHelper: PanelsLayoutHelper, traceSamples: ArrayList<TraceSample>): JPanel {

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
        val icon = if (percentile.previousDuration!!.raw > percentile.currentDuration.raw) Laf.Icons.Insight.SPAN_DURATION_DROPPED else Laf.Icons.Insight.SPAN_DURATION_ROSE
        val durationText = computeDurationText(percentile)
        val whenText = computeWhenText(percentile)
        val durationLabelText = asHtml(spanGrayed("$durationText,$whenText"))
        val durationLabel = JBLabel(durationLabelText, icon, SwingConstants.LEFT)
        durationLabel.toolTipText = durationLabelText
        durationsPanel.add(durationLabel, BorderLayout.CENTER)
    }

    if (percentile.changeTime != null && (percentile.changeVerified == null || percentile.changeVerified == false)) {

        val evalLabel = JBLabel("Evaluating")
        evalLabel.toolTipText = "This change is still being validated and is based on initial data."
        evalLabel.horizontalAlignment = SwingConstants.RIGHT
        //the evalLabel wants to be aligned with the insights icons panels, so it takes its width from there
        val evalPanel = InsightAlignedPanel(panelsLayoutHelper)
        evalPanel.layout = BorderLayout()
        evalPanel.add(evalLabel, BorderLayout.CENTER)
        evalPanel.isOpaque = false
        addCurrentLargestWidthIconPanel(panelsLayoutHelper, evalPanel.preferredSize.width)
        durationsPanel.add(evalPanel, BorderLayout.EAST)
    }

    return durationsPanel
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

fun buildJPanelWithButtonToJaeger(builder: StringBuilder, line: JPanel, traceSample: TraceSample?,
                                  project: Project, spanName: String): JPanel {
    val spanFlowLabel = CopyableLabelHtml(asHtml(builder.toString()))
    spanFlowLabel.alignmentX = 0.0f
    line.add(spanFlowLabel, BorderLayout.CENTER)

    val buttonToJaeger = buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample))
    if (buttonToJaeger != null) {
        val wrapper = JPanel(BorderLayout())
        wrapper.isOpaque = false
        wrapper.add(buttonToJaeger, BorderLayout.NORTH)
        line.add(wrapper, BorderLayout.EAST)
    }
    return line
}

fun buildTraceSample(percentile: SpanDurationsPercentile): TraceSample {
    val percentileName = "P${(percentile.percentile * 100).toInt()}"
    var traceId = ""
    if (!percentile.traceIds.isNullOrEmpty()) {
        traceId = percentile.traceIds!!.first()
    }
    return TraceSample(percentileName, traceId)
}

private fun computeWhenText(percentile: SpanDurationsPercentile): String {
    val current = PrettyTime(Timestamp(System.currentTimeMillis()))
    return current.format(current.approximateDuration(percentile.changeTime))
}

private fun computeDurationText(percentile: SpanDurationsPercentile): String {

    val durationMillis =
            TimeUnit.MILLISECONDS.convert(
                    abs(percentile.previousDuration!!.raw - percentile.currentDuration.raw),
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
    layoutHelper.addObjectAttribute("SpanDurationsDurationPLabel", "largestWidth",
            max(currentLargest, width))
}