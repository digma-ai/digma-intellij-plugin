package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import org.digma.intellij.plugin.model.rest.insights.SpanInfo
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.getHex
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.TraceSample
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

fun spanDurationPanel(
        project: Project,
        spanDurationsInsight: SpanDurationsInsight,
        panelsLayoutHelper: PanelsLayoutHelper
): JPanel {

    if (spanDurationsInsight.percentiles.isEmpty()) {
        return createInsightPanel(
                project = project,
                insight = spanDurationsInsight,
                title = "Duration",
                description = "Waiting for more data.",
                iconsList = listOf(Laf.Icons.Insight.WAITING_DATA),
                bodyPanel = null,
                buttons = null,
                paginationComponent = null
        )
    }

    val durationsListPanel = JBPanel<JBPanel<*>>()
    durationsListPanel.layout = BoxLayout(durationsListPanel, BoxLayout.Y_AXIS)
    durationsListPanel.isOpaque = false

    val traceSamples = ArrayList<TraceSample>()

    spanDurationsInsight.percentiles
            .sortedBy(SpanDurationsPercentile::percentile)
            .forEach { percentile: SpanDurationsPercentile ->
                val durationsPanel = percentileRowPanel(percentile, panelsLayoutHelper, traceSamples)
                durationsListPanel.add(durationsPanel)
            }


    val buttonToGraph = buildButtonToPercentilesGraph(project, spanDurationsInsight.spanInfo)
    //related to issue #621
    //val buttonToJaeger = buildButtonToJaeger(project, "Compare", spanDurationsInsight.spanInfo.name, traceSamples)

    return createInsightPanel(
            project = project,
            insight = spanDurationsInsight,
            title = "Duration",
            description = "",
            iconsList = listOf(Laf.Icons.Insight.DURATION),
            bodyPanel = durationsListPanel,
            buttons = listOf(buttonToGraph),
            paginationComponent = null
    )
}

private fun buildButtonToPercentilesGraph(project: Project, span: SpanInfo): JButton {
    val analyticsService = AnalyticsService.getInstance(project)
    val button = ListItemActionButton("Histogram")
    button.addActionListener {
        val htmlContent = analyticsService.getHtmlGraphForSpanPercentiles(span.instrumentationLibrary, span.name, Laf.Colors.PLUGIN_BACKGROUND.getHex())
        DigmaHTMLEditorProvider.openEditor(project, "Percentiles Graph of Span ${span.name}", htmlContent)
    }

    return button
}