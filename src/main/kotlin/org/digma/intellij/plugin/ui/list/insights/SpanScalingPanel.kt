package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.JBUI.Borders.emptyBottom
import com.intellij.util.ui.JBUI.Borders.emptyTop
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.editor.getCurrentPageNumberForInsight
import org.digma.intellij.plugin.editor.updateListOfEntriesToDisplay
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.AffectedEndpointInfo
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema
import org.digma.intellij.plugin.model.rest.insights.RootCauseSpan
import org.digma.intellij.plugin.model.rest.insights.SpanScalingInsight
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.getHex
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

private const val RECORDS_PER_PAGE_AFFECTED_ENDPOINTS = 3

fun spanScalingPanel(project: Project, insight: SpanScalingInsight): JPanel {
    val scalingPanel = createDefaultBoxLayoutYAxisPanel()
    scalingPanel.add(getScalingCalculationsPanel(insight))
    scalingPanel.border = emptyBottom(2)

    if (!insight.rootCauseSpans.isNullOrEmpty()) {
        scalingPanel.add(getRootCauseSpansPanel(project, insight))
    }

    if (!insight.affectedEndpoints.isNullOrEmpty()) {
        scalingPanel.add(getAffectedEndpointsPanel(project, insight))
    }

    val buttonToGraph = buildButtonToScalingGraph(project, insight.spanName, insight.spanInstrumentationLibrary, insight.type)

    val backwardsCompatibilityTitle = "Scaling Issue Found"
    val backwardsCompatibilityDescription = "Significant performance degradation at ${insight.turningPointConcurrency} executions/second"

    return createInsightPanel(
        project = project,
        insight = insight,
        title = insight.shortDisplayInfo?.title ?: backwardsCompatibilityTitle,
        description = insight.shortDisplayInfo?.description ?: backwardsCompatibilityDescription,
        iconsList = listOf(Laf.Icons.Insight.SCALE),
        bodyPanel = scalingPanel,
        buttons = listOf(buttonToGraph),
        paginationComponent = null
    )
}


fun getRootCauseSpansPanel(project: Project, insight: SpanScalingInsight): JPanel {

    val rootCauseSpansPanel = createDefaultBoxLayoutYAxisPanel()
    rootCauseSpansPanel.border = emptyTop(5)

    val causedByLabel = JLabel(asHtml(spanGrayed("Caused By:")), SwingConstants.LEFT)

    val causedByPanel = JPanel(BorderLayout())
    causedByPanel.border = empty()
    causedByPanel.isOpaque = false
    causedByPanel.add(causedByLabel, BorderLayout.WEST)
    rootCauseSpansPanel.add(causedByPanel)

    insight.rootCauseSpans!!.let { spans ->
        repeat(spans.size) { index ->
            rootCauseSpansPanel.add(getRootCauseSpanPanel(project, spans[index]))
        }
    }

    return rootCauseSpansPanel
}

fun getAffectedEndpointsPanel(project: Project, insight: SpanScalingInsight): JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type + "endpoints"
    val paginationPanel = JPanel()
    val lastPageNum = countNumberOfPages(insight.affectedEndpoints!!.size, RECORDS_PER_PAGE_AFFECTED_ENDPOINTS)
    val affectedEndpointsToDisplay = ArrayList<AffectedEndpointInfo>()
    var affectedEndpointsPanel: DigmaResettablePanel? = null

    affectedEndpointsPanel = object : DigmaResettablePanel() {
        override fun reset() {
            affectedEndpointsPanel!!.removeAll()
            buildAffectedEndpointList(affectedEndpointsPanel!!, affectedEndpointsToDisplay, project)
            rebuildPaginationPanel(
                paginationPanel,
                lastPageNum,
                insight.affectedEndpoints!!,
                affectedEndpointsPanel,
                affectedEndpointsToDisplay,
                uniqueInsightId,
                RECORDS_PER_PAGE_AFFECTED_ENDPOINTS,
                project,
                insight.type
            )
        }
    }
    affectedEndpointsPanel.border = emptyTop(5)

    updateListOfEntriesToDisplay(
        insight.affectedEndpoints!!,
        affectedEndpointsToDisplay,
        getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum),
        RECORDS_PER_PAGE_AFFECTED_ENDPOINTS,
        project
    )
    buildAffectedEndpointList(affectedEndpointsPanel, affectedEndpointsToDisplay, project)

    return affectedEndpointsPanel
}

private fun buildAffectedEndpointList(
    rootCausePanel: DigmaResettablePanel,
    affectedEndpointToDisplay: List<AffectedEndpointInfo>,
    project: Project
) {
    rootCausePanel.layout = BoxLayout(rootCausePanel, BoxLayout.Y_AXIS)
    rootCausePanel.isOpaque = false

    val labelPanel = JPanel(BorderLayout())

    labelPanel.border = empty()
    labelPanel.isOpaque = false

    val label = JLabel(asHtml(spanGrayed("Affected Endpoints:")), SwingConstants.LEFT)

    label.isOpaque = false
    labelPanel.add(label)

    rootCausePanel.add(labelPanel)

    affectedEndpointToDisplay.forEach { affEndpoint: AffectedEndpointInfo ->
        rootCausePanel.add(buildAffectedEndpointItem(project,  affEndpoint))
    }
}

private fun buildAffectedEndpointItem(project: Project, affectedEndpoint: AffectedEndpointInfo): JPanel {
    val endPointPanel = JPanel(BorderLayout())
    endPointPanel.border = empty()
    endPointPanel.isOpaque = false

    val routeInfo = EndpointSchema.getRouteInfo(affectedEndpoint.route)
    val shortRouteName = routeInfo.shortName

    val normalizedDisplayName = StringUtils.normalizeSpace(shortRouteName)
    val link = ActionLink(normalizedDisplayName) {
        ActivityMonitor.getInstance(project).registerSpanLinkClicked(InsightType.SpanScaling)
        project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(affectedEndpoint.spanCodeObjectId)
    }

    link.toolTipText = asHtml(shortRouteName)
    endPointPanel.add(link, BorderLayout.NORTH)

    val sampleTraceId = affectedEndpoint.sampleTraceId
    val traceSample = TraceSample(affectedEndpoint.name, sampleTraceId)
    val buttonToJaeger = buildButtonToJaeger(project, "Trace", affectedEndpoint.name, listOf(traceSample), InsightType.SpanScaling)
    if (buttonToJaeger != null) {
        endPointPanel.add(buttonToJaeger, BorderLayout.EAST)
    }

    return endPointPanel
}

fun getRootCauseSpanPanel(project: Project, rootCauseSpan: RootCauseSpan): JPanel {

    val rootCausePanel = JPanel(BorderLayout())
    rootCausePanel.border = empty()
    rootCausePanel.isOpaque = false

    val normalizedDisplayName = StringUtils.normalizeSpace(rootCauseSpan.displayName)

    val spanId = rootCauseSpan.spanCodeObjectId
    val link =
        ActionLink(normalizedDisplayName) {
            ActivityMonitor.getInstance(project).registerSpanLinkClicked(MonitoredPanel.Summary)
            project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(spanId)
        }
    link.toolTipText = asHtml(normalizedDisplayName)
    rootCausePanel.add(link, BorderLayout.CENTER)

    val spanName = rootCauseSpan.name
    val sampleTraceId = rootCauseSpan.sampleTraceId
    val traceSample = TraceSample(spanName, sampleTraceId)
    val buttonToJaeger = buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample), InsightType.SpanScaling)
    if (buttonToJaeger != null) {
        rootCausePanel.add(buttonToJaeger, BorderLayout.EAST)
    }

    return rootCausePanel
}

private fun getScalingCalculationsPanel(insight: SpanScalingInsight): JPanel {
    val scalingBodyPanel = createDefaultBoxLayoutLineAxisPanel()

    val concurrencyLabel = JLabel(asHtml("Tested concurrency: ${spanBold(insight.maxConcurrency.toString())}"))
    val durationLabel = JLabel(
        asHtml(
            "Duration: " +
                    spanBold("${insight.minDuration.value} ${insight.minDuration.unit} - ${insight.maxDuration.value} ${insight.maxDuration.unit}")
        )
    )

    scalingBodyPanel.add(concurrencyLabel)
    scalingBodyPanel.add(Box.createHorizontalGlue())
    scalingBodyPanel.add(durationLabel)
    return scalingBodyPanel
}

private fun buildButtonToScalingGraph(project: Project, spanName: String, instLibrary: String, insightType: InsightType): JButton {
    val analyticsService = AnalyticsService.getInstance(project)
    val button = ListItemActionButton("Histogram")
    button.addActionListener {

        runBackgroundableTask("Open histogram", project, true) {
            try {
                val htmlContent = analyticsService.getHtmlGraphForSpanScaling(instLibrary, spanName, Laf.Colors.PLUGIN_BACKGROUND.getHex())
                ActivityMonitor.getInstance(project).registerButtonClicked("histogram", insightType)
                EDT.ensureEDT {
                    DigmaHTMLEditorProvider.openEditor(project, "Scaling Graph of Span $spanName", htmlContent)
                }
            }catch (e: AnalyticsServiceException){
                //do nothing, the exception is logged in AnalyticsService
            }
        }


    }

    return button
}