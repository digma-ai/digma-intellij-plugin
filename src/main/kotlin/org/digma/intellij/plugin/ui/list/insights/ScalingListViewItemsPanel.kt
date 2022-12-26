package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.rest.insights.*
import org.digma.intellij.plugin.ui.common.CopyableLabelHtmlWithForegroundColor
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanBold
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.JPanel

fun spanScalingListViewItemsPanel(project: Project, insight: SpanScalingInsight): JPanel {
    val scalingPanel = createDefaultBoxLayoutYAxisPanel()
    scalingPanel.add(getScalingDescriptionPanel(insight))
    scalingPanel.add(getScalingCalculationsPanel(insight))

    return createInsightPanel(
            project = project,
            insight = insight,
            title = "Scaling Issue Found",
            description = "",
            iconsList = listOf(Laf.Icons.Insight.SCALE),
            bodyPanel = scalingPanel,
            buttons = null,
            paginationComponent = null
    )
}

private fun getScalingDescriptionPanel(insight: SpanScalingInsight): CopyableLabelHtmlWithForegroundColor {
    val description = "Significant performance degradation at ${insight.turningPointConcurrency} executions/second"
    return CopyableLabelHtmlWithForegroundColor(description, Laf.Colors.GRAY)
}

private fun getScalingCalculationsPanel(insight: SpanScalingInsight): JPanel {
    val scalingBodyPanel = createDefaultBoxLayoutLineAxisPanel()

    val concurrencyLabel = JLabel(asHtml("Tested concurrency: ${spanBold(insight.maxConcurrency.toString())}"))
    val durationLabel = JLabel(asHtml("Duration: " +
            spanBold("${insight.minDuration.value} ${insight.minDuration.unit} - ${insight.maxDuration.value} ${insight.maxDuration.unit}")))

    scalingBodyPanel.add(concurrencyLabel)
    scalingBodyPanel.add(Box.createHorizontalGlue())
    scalingBodyPanel.add(durationLabel)
    return scalingBodyPanel
}