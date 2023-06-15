package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import org.digma.intellij.plugin.model.rest.insights.SpanInfo
import org.digma.intellij.plugin.model.rest.insights.SpanInstanceInfo
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.toolwindow.recentactivity.RecentActivityService
import org.digma.intellij.plugin.ui.common.IconWithLiveIndication
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.getHex
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.ListItemActionIconButton
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.scaled
import java.time.Instant
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JPanel

fun spanDurationPanel(
    project: Project,
    spanDurationsInsight: SpanDurationsInsight,
    panelsLayoutHelper: PanelsLayoutHelper,
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

    var icon: Icon = Laf.Icons.Insight.DURATION

    spanDurationsInsight.lastSpanInstanceInfo?.let {
        val lastCallPanel = createLastCallPanel(it)
        durationsListPanel.add(lastCallPanel)

        if (it.startTime.toInstant().isAfter(Instant.now().minusSeconds(60))) {
            icon = IconWithLiveIndication(icon)
        }
    }

    val traceSamples = ArrayList<TraceSample>()

    spanDurationsInsight.percentiles
        .sortedBy(SpanDurationsPercentile::percentile)
        .forEach { percentile: SpanDurationsPercentile ->
            val durationsPanel = percentileRowPanel(percentile, panelsLayoutHelper, traceSamples)
            durationsListPanel.add(durationsPanel)
        }


    val buttonToGraph = buildButtonToPercentilesGraph(project, spanDurationsInsight.spanInfo, spanDurationsInsight.type)
    val liveViewButton = buildLiveViewButton(project, spanDurationsInsight)
    //related to issue #621
    //val buttonToJaeger = buildButtonToJaeger(project, "Compare", spanDurationsInsight.spanInfo.name, traceSamples)

    return createInsightPanel(
        project = project,
        insight = spanDurationsInsight,
        title = "Duration",
        description = "",
        iconsList = listOf(icon),
        bodyPanel = durationsListPanel,
        buttons = listOf(buttonToGraph, liveViewButton),
        paginationComponent = null
    )
}


private fun buildLiveViewButton(project: Project, spanDurationsInsight: SpanDurationsInsight): JButton {

    val icon = if (JBColor.isBright()) Laf.Icons.Common.LiveIconLight else Laf.Icons.Common.LiveIconDark
    val borderColor = if (JBColor.isBright()) Laf.Colors.LIVE_BUTTON_BORDER_LIGHT else Laf.Colors.LIVE_BUTTON_BORDER_DARK
    val liveViewButton = ListItemActionIconButton("Live", icon)
    liveViewButton.isBorderPainted = true
    liveViewButton.border = JBUI.Borders.customLine(borderColor, 2.scaled())
    liveViewButton.addActionListener {
        try {
            ActivityMonitor.getInstance(project).registerInsightButtonClicked("live", spanDurationsInsight.type)
            val idToUse = spanDurationsInsight.prefixedCodeObjectId
            idToUse?.let {
                val durationLiveData = AnalyticsService.getInstance(project).getDurationLiveData(it)
                RecentActivityService.getInstance(project).sendLiveData(durationLiveData, it)
            }
        } catch (e: AnalyticsServiceException) {
            //do nothing, the exception is logged in AnalyticsService
        }
    }
    return liveViewButton
}


private fun buildButtonToPercentilesGraph(project: Project, span: SpanInfo, insightType: InsightType): JButton {
    val analyticsService = AnalyticsService.getInstance(project)
    val button = ListItemActionButton("Histogram")
    button.addActionListener {
        ActivityMonitor.getInstance(project).registerInsightButtonClicked("histogram", insightType)

        val htmlContent =
            analyticsService.getHtmlGraphForSpanPercentiles(span.instrumentationLibrary, span.name, Laf.Colors.PLUGIN_BACKGROUND.getHex())
        DigmaHTMLEditorProvider.openEditor(project, "Percentiles Graph of Span ${span.name}", htmlContent)
    }

    return button
}

private fun createLastCallPanel(lastSpan: SpanInstanceInfo): JPanel {
    val contentsSb = StringBuilder()
    contentsSb.append("Last call: $HTML_NON_BREAKING_SPACE ")
    contentsSb.append(spanBold(evalDuration(lastSpan.duration)))
    contentsSb.append(" $HTML_NON_BREAKING_SPACE ")
    contentsSb.append(spanBold(CommonUtils.prettyTimeOf(lastSpan.startTime)))

    val thePanel = panel {
        row {
            text(contentsSb.toString())
                .resizableColumn()
        }.resizableRow()
    }.andTransparent()

    thePanel.border = JBUI.Borders.empty()

    return thePanel
}
