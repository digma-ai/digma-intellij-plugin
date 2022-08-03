package org.digma.intellij.plugin.ui.list.insights

import com.google.common.io.CharStreams
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.containers.isNullOrEmpty
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.WrapLayout
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import org.digma.intellij.plugin.model.rest.insights.SpanFlow
import org.digma.intellij.plugin.model.rest.insights.SpanInfo
import org.digma.intellij.plugin.model.rest.insights.SpanUsagesInsight
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.common.CopyableLabel
import org.digma.intellij.plugin.ui.common.CopyableLabelHtml
import org.digma.intellij.plugin.ui.common.Html.ARROW_RIGHT
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.span
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.TraceSample
import org.ocpsoft.prettytime.PrettyTime
import org.threeten.extra.AmountFormats
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.InputStreamReader
import java.sql.Timestamp
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.swing.JButton
import javax.swing.JLabel
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

fun spanUsagesPanel(project: Project, spanUsagesInsight: SpanUsagesInsight): JPanel {

    val title = JLabel(asHtml(spanBold("Top Usage")), SwingConstants.LEFT)
    title.isOpaque = false

    val flowsListPanel = JBPanel<JBPanel<*>>()
    flowsListPanel.layout = GridLayout(spanUsagesInsight.flows.size, 1, 0, 3)
    flowsListPanel.border = empty()
    flowsListPanel.isOpaque = false

    spanUsagesInsight.flows.forEach { spanFlow: SpanFlow ->

        val builder = StringBuilder("${span(String.format("%.1f", spanFlow.percentage))}% ")

        var spanName = spanUsagesInsight.span // default, just in case first service is not found
        spanFlow.firstService?.let { firstService ->
            builder.append(spanGrayed(firstService.service + ": "))
            builder.append(span(firstService.span))
            spanName = firstService.span
        }
        spanFlow.intermediateSpan?.let { intermediateSpan ->
            builder.append(" ${spanGrayed(ARROW_RIGHT)} ")
            builder.append(span(intermediateSpan))
        }
        spanFlow.lastService?.let { lastService ->
            builder.append(" ${spanGrayed(ARROW_RIGHT)} ")
            builder.append(spanGrayed(lastService.service + ": "))
            builder.append(span(lastService.span))
        }
        spanFlow.lastServiceSpan?.let { lastServiceSpan ->
            builder.append(" ${spanGrayed(ARROW_RIGHT)} ")
            builder.append(span(lastServiceSpan))
        }

        val label = CopyableLabelHtml(asHtml(builder.toString()))
        label.alignmentX = 0.0f

        var traceSample: TraceSample? = null
        spanFlow.sampleTraceIds.firstOrNull()?.let { sampleTraceId ->
            traceSample = TraceSample(spanName, sampleTraceId)
        }
        val buttonToJaeger = buildButtonToJaeger(project, "Trace", spanName, traceSample)
        if (buttonToJaeger == null) {
            flowsListPanel.add(label)
        } else {
            val wrapperPanel = JBPanel<JBPanel<*>>()
            wrapperPanel.andTransparent()
            wrapperPanel.layout = WrapLayout(FlowLayout.LEFT, 3, 3)
            wrapperPanel.add(label)
            wrapperPanel.add(buttonToJaeger)

            flowsListPanel.add(wrapperPanel)
        }

    }


    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.add(title, BorderLayout.NORTH)
    result.add(flowsListPanel, BorderLayout.CENTER)

    return insightItemPanel(result)

}


fun spanDurationPanel(
    project: Project,
    spanDurationsInsight: SpanDurationsInsight,
    panelsLayoutHelper: PanelsLayoutHelper
): JPanel {

    if (spanDurationsInsight.percentiles.isEmpty()) {
        return createInsightPanel("Duration", "Waiting for more data.", Laf.Icons.Insight.WAITING_DATA, "", panelsLayoutHelper)
    }

    val title = JLabel(asHtml(spanBold("Duration")), SwingConstants.LEFT)

    val durationsListPanel = JBPanel<JBPanel<*>>()
    durationsListPanel.layout = GridLayout(spanDurationsInsight.percentiles.size, 1, 0, 2)
    durationsListPanel.isOpaque = false

    val sortedPercentiles = spanDurationsInsight.percentiles.sortedBy(SpanDurationsPercentile::percentile)

    val tolerationConstant: Long = 10000

    val traceSamples = ArrayList<TraceSample>()

    sortedPercentiles.forEach { percentile: SpanDurationsPercentile ->

        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
        var changeMeaningfulEnough = false
        val durationsPanel = JBPanel<JBPanel<*>>()
        durationsPanel.layout = BorderLayout(5, 0)
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
        durationsPanel.add(pLabelPanel, BorderLayout.WEST)


        if (percentile.previousDuration != null && percentile.changeTime != null) {

            val rawDiff: Long = abs(percentile.currentDuration.raw - percentile.previousDuration!!.raw)
            changeMeaningfulEnough = ((rawDiff / percentile.previousDuration!!.raw) > 0.1) && (rawDiff > tolerationConstant)
            if (changeMeaningfulEnough) {
                val icon = if (percentile.previousDuration!!.raw > percentile.currentDuration.raw) Laf.Icons.Insight.SPAN_DURATION_DROPPED else Laf.Icons.Insight.SPAN_DURATION_ROSE
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
            val evalPanel = InsightAlignedPanel(panelsLayoutHelper)
            evalPanel.layout = BorderLayout()
            evalPanel.add(evalLabel, BorderLayout.CENTER)
            evalPanel.isOpaque = false
            addCurrentLargestWidthIconPanel(panelsLayoutHelper, evalPanel.preferredSize.width)
            durationsPanel.add(evalPanel, BorderLayout.EAST)
        }

        durationsListPanel.add(durationsPanel)

    }

    val buttonToGraph = buildButtonToPercentilesGraph(project, spanDurationsInsight.span)
    val buttonToJaeger = buildButtonToJaeger(project, "Compare", spanDurationsInsight.span.name, traceSamples)
    val iconPanel = buildIconPanelWithLinks(buttonToGraph, buttonToJaeger)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.isOpaque = false
    result.add(title, BorderLayout.NORTH)
    result.add(durationsListPanel, BorderLayout.CENTER)
    result.add(iconPanel, BorderLayout.EAST)
    return insightItemPanel(result)
}

fun buildIconPanelWithLinks(
    buttonToPercentilesGraph: JButton, buttonToJaeger: JButton?
): JBPanel<*> {

    val iconPanel = panel {
        row {
            icon(Laf.Icons.Insight.HISTOGRAM)
                .horizontalAlign(HorizontalAlign.CENTER)
        }
        row {
            cell(buttonToPercentilesGraph)
        }
        if (buttonToJaeger != null) {
            row {
                cell(buttonToJaeger)
            }
        }
    }.andTransparent()
    return iconPanel
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

    val button = ActionLink(linkCaption)
    button.addActionListener {
        HTMLEditorProvider.openEditor(project, editorTitle,  htmlContent)
    }

    return button
}

// if cannot create the button then would return null
fun buildButtonToJaeger(
    project: Project, linkCaption: String, spanName: String, traceSample: TraceSample?
): JButton? {
    if (traceSample == null) {
        return null
    }
    return buildButtonToJaeger(project, linkCaption, spanName, listOf(traceSample))
}

fun buildButtonToPercentilesGraph(project: Project, span: SpanInfo): ActionLink {
    val analyticsService = AnalyticsService.getInstance(project)
    val button = ActionLink("Histogram")
    button.addActionListener {
        val htmlContent = analyticsService.getHtmlGraphForSpanPercentiles(span.instrumentationLibrary, span.name)
        HTMLEditorProvider.openEditor(project, "Percentiles Graph of Span ${span.name}", htmlContent)
    }

    return button
}

fun buildTraceSample(percentile: SpanDurationsPercentile): TraceSample {
    val percentileName = "P${(percentile.percentile * 100).toInt()}"
    var traceId = ""
    if (!percentile.traceIds.isNullOrEmpty()) {
        traceId = percentile.traceIds.first()
    }
    return TraceSample(percentileName, traceId)
}

private fun computeWhenText(percentile: SpanDurationsPercentile): String {
    val current = PrettyTime(Timestamp(System.currentTimeMillis()))
    return current.format(current.calculatePreciseDuration(percentile.changeTime))
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


private fun getCurrentLargestWidthDurationPLabel(layoutHelper: PanelsLayoutHelper, width: Int): Int {
    //this method should never return null and never throw NPE
    val currentLargest: Int =
        (layoutHelper.getObjectAttribute("SpanDurationsDurationPLabel", "largestWidth") ?: 0) as Int
    return max(width, currentLargest)
}

private fun addCurrentLargestWidthDurationPLabel(layoutHelper: PanelsLayoutHelper, width: Int) {
    //this method should never throw NPE
    val currentLargest: Int =
        (layoutHelper.getObjectAttribute("SpanDurationsDurationPLabel", "largestWidth") ?: 0) as Int
    layoutHelper.addObjectAttribute("SpanDurationsDurationPLabel", "largestWidth",
        max(currentLargest, width))
}