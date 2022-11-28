package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.model.rest.insights.*
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.TraceSample
import java.awt.GridLayout
import javax.swing.*

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
    durationsListPanel.layout = GridLayout(spanDurationsInsight.percentiles.size, 1, 0, 2)
    durationsListPanel.isOpaque = false

    val traceSamples = ArrayList<TraceSample>()

    spanDurationsInsight.percentiles
            .sortedBy(SpanDurationsPercentile::percentile)
            .forEach { percentile: SpanDurationsPercentile ->
                val durationsPanel = percentileRowPanel(percentile, panelsLayoutHelper, traceSamples)
                durationsListPanel.add(durationsPanel)
            }

    val buttonToGraph = buildButtonToPercentilesGraph(project, spanDurationsInsight.span)
    val buttonToJaeger = buildButtonToJaeger(project, "Compare", spanDurationsInsight.span.name, traceSamples)

    return createInsightPanel(
            project = project,
            insight = spanDurationsInsight,
            title = "Duration",
            description = "",
            iconsList = listOf(Laf.Icons.Insight.DURATION),
            bodyPanel = durationsListPanel,
            buttons = listOf(buttonToGraph, buttonToJaeger),
            paginationComponent = null
    )
}

private fun buildButtonToPercentilesGraph(project: Project, span: SpanInfo): JButton {
    val analyticsService = AnalyticsService.getInstance(project)
    val button = ListItemActionButton("Histogram")
    button.addActionListener {
        val htmlContent = analyticsService.getHtmlGraphForSpanPercentiles(span.instrumentationLibrary, span.name, Laf.Colors.PLUGIN_BACKGROUND.getHex())
        HTMLEditorProvider.openEditor(project, "Percentiles Graph of Span ${span.name}", htmlContent)
    }

    return button
}