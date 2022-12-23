package org.digma.intellij.plugin.ui.list.insights

import com.google.common.io.CharStreams
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import org.digma.intellij.plugin.settings.LinkMode
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.common.CopyableLabel
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.service.needToShowDurationChange
import org.ocpsoft.prettytime.PrettyTime
import org.threeten.extra.AmountFormats
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.InputStreamReader
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.abs
import kotlin.math.max

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
    durationsPanel.layout = BoxLayout(durationsPanel, BoxLayout.LINE_AXIS)
    durationsPanel.border = empty()
    durationsPanel.isOpaque = false

    val percentileName = "P${(percentile.percentile * 100).toInt()}"
    traceSamples.add(buildTraceSample(percentile))
    val pLabelText = "$percentileName ${percentile.currentDuration.value} ${percentile.currentDuration.unit}"
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
            addCurrentLargestWidthDurationPLabel(panelsLayoutHelper, w)
            return Dimension(getCurrentLargestWidthDurationPLabel(panelsLayoutHelper, w), h)
        }
    }
    pLabelPanel.layout = BorderLayout()
    pLabelPanel.border = empty()
    pLabelPanel.isOpaque = false
    pLabelPanel.add(pLabel, BorderLayout.WEST)
    addCurrentLargestWidthDurationPLabel(panelsLayoutHelper, pLabelPanel.preferredSize.width)
    durationsPanel.add(pLabelPanel)

    if (needToShowDurationChange(percentile)) {
        val icon = if (percentile.previousDuration!!.raw > percentile.currentDuration.raw) Laf.Icons.Insight.SPAN_DURATION_DROPPED else Laf.Icons.Insight.SPAN_DURATION_ROSE
        val durationText = computeDurationText(percentile)
        val whenText = computeWhenText(percentile)
        val durationLabelText = asHtml(spanGrayed("$durationText,$whenText"))
        val durationLabel = JBLabel(durationLabelText, icon, SwingConstants.LEFT)
        durationLabel.toolTipText = durationLabelText
        durationsPanel.add(durationLabel)
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
        durationsPanel.add(evalPanel)
    }

    return durationsPanel
}

// if cannot create the button then would return null
fun buildButtonToJaeger(
    project: Project, linkCaption: String, spanName: String, traceSamples: List<TraceSample>
): JButton? {

    val settingsState = SettingsState.getInstance(project)

    val jaegerBaseUrl = settingsState.jaegerUrl?.trim()?.trimEnd('/')
    if (jaegerBaseUrl.isNullOrBlank() || traceSamples.isNullOrEmpty()) {
        return null
    }
    val filtered = traceSamples.filter { x -> x.hasTraceId() }
    if (filtered.isNullOrEmpty()) {
        return null
    }

    val caption: String
    val jaegerUrl: String
    val embedPart = "&uiEmbed=v0"

    val trace1 = filtered[0].traceId?.lowercase()
    if (filtered.size == 1) {
        caption = "A sample ${filtered[0].traceName} trace"
        jaegerUrl = "${jaegerBaseUrl}/trace/${trace1}?cohort=${trace1}${embedPart}"
    } else {
        // assuming it has (at least) size of 2
        val trace2 = filtered[1].traceId?.lowercase()
        caption = "Comparing: A sample ${filtered[0].traceName} trace with a ${filtered[1].traceName} trace"
        jaegerUrl = "${jaegerBaseUrl}/trace/${trace1}...${trace2}?cohort=${trace1}&cohort=${trace2}${embedPart}"
    }

    val htmlContent = SpanPanels.JAEGER_EMBEDDED_HTML_TEMPLATE
        .replace("__JAEGER_EMBEDDED_URL__", jaegerUrl)
        .replace("__CAPTION__", caption)

    val editorTitle = "Jaeger sample traces of Span ${spanName}"

    val button = ListItemActionButton(linkCaption)
    if (settingsState.jaegerLinkMode == LinkMode.Internal) {
        button.addActionListener {
            HTMLEditorProvider.openEditor(project, editorTitle, htmlContent)
        }
    } else {
        // handle LinkMode.External
        button.addActionListener {
            BrowserUtil.browse(jaegerUrl, project)
        }
    }

    return button
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
    val javaDuration = java.time.Duration.ofMillis(durationMillis)
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