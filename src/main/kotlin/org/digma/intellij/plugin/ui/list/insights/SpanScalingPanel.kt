package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.JBUI.Borders.emptyBottom
import com.intellij.util.ui.JBUI.Borders.emptyTop
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.editor.getCurrentPageNumberForInsight
import org.digma.intellij.plugin.editor.updateListOfEntriesToDisplay
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.AffectedEndpointInfo
import org.digma.intellij.plugin.model.rest.insights.EndpointSchema
import org.digma.intellij.plugin.model.rest.insights.RootCauseSpan
import org.digma.intellij.plugin.model.rest.insights.SpanScalingInsight
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.getHex
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.openWorkspaceFileForSpan
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import javax.swing.*

private const val RECORDS_PER_PAGE_AFFECTED_ENDPOINTS = 3

fun spanScalingPanel(project: Project, insight: SpanScalingInsight, moreData: HashMap<String, Any>): JPanel {
    val scalingPanel = createDefaultBoxLayoutYAxisPanel()
    scalingPanel.add(getScalingCalculationsPanel(insight))
    scalingPanel.border = emptyBottom(2)

    if (!insight.rootCauseSpans.isNullOrEmpty()) {
        scalingPanel.add(getRootCauseSpansPanel(project, moreData, insight))
    }

    if (!insight.affectedEndpoints.isNullOrEmpty()) {
        scalingPanel.add(getAffectedEndpointsPanel(project, moreData, insight))
    }

    val buttonToGraph = buildButtonToScalingGraph(project, insight.spanName,insight.spanInstrumentationLibrary, insight.type)

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



fun getRootCauseSpansPanel(project: Project, moreData: HashMap<String, Any>, insight: SpanScalingInsight): JPanel {

    val rootCauseSpansPanel = createDefaultBoxLayoutYAxisPanel()
    rootCauseSpansPanel.border = emptyTop(5)

    val causedByLabel = JLabel(asHtml(spanGrayed("Caused By:")), SwingConstants.LEFT)

    val causedByPanel = JPanel(BorderLayout())
    causedByPanel.border = empty()
    causedByPanel.isOpaque = false
    causedByPanel.add(causedByLabel,BorderLayout.WEST)
    rootCauseSpansPanel.add(causedByPanel)

    insight.rootCauseSpans!!.let { spans ->
        repeat(spans.size) {index ->
            rootCauseSpansPanel.add(getRootCauseSpanPanel(project,moreData,spans[index]))
        }
    }

    return rootCauseSpansPanel
}

fun getAffectedEndpointsPanel(project: Project, moreData: HashMap<String, Any>, insight: SpanScalingInsight) : JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type + "endpoints"
    val paginationPanel = JPanel()
    val lastPageNum = countNumberOfPages(insight.affectedEndpoints!!.size, RECORDS_PER_PAGE_AFFECTED_ENDPOINTS)
    val affectedEndpointsToDisplay = ArrayList<AffectedEndpointInfo>()
    var affectedEndpointsPanel: DigmaResettablePanel? = null

    affectedEndpointsPanel = object : DigmaResettablePanel() {
        override fun reset() {
            affectedEndpointsPanel!!.removeAll()
            buildAffectedEndpointList(affectedEndpointsPanel!!, affectedEndpointsToDisplay, project, moreData)
            rebuildPaginationPanel(paginationPanel, lastPageNum,
                insight.affectedEndpoints!!, affectedEndpointsPanel, affectedEndpointsToDisplay, uniqueInsightId, RECORDS_PER_PAGE_AFFECTED_ENDPOINTS, project, insight.type)
        }
    }
    affectedEndpointsPanel.border = emptyTop(5)

    updateListOfEntriesToDisplay(insight.affectedEndpoints!!, affectedEndpointsToDisplay, getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum), RECORDS_PER_PAGE_AFFECTED_ENDPOINTS, project)
    buildAffectedEndpointList(affectedEndpointsPanel, affectedEndpointsToDisplay, project, moreData)

    return affectedEndpointsPanel
}

private fun buildAffectedEndpointList(
    rootCausePanel: DigmaResettablePanel,
    affectedEndpointToDisplay: List<AffectedEndpointInfo>,
    project: Project,
    moreData: HashMap<String, Any>
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
        rootCausePanel.add(buildAffectedEndpointItem(project, moreData, affEndpoint))
    }
}

private fun buildAffectedEndpointItem(project: Project, moreData: HashMap<String, Any>, affectedEndpoint: AffectedEndpointInfo): JPanel{
    val endPointPanel = JPanel(BorderLayout())
    endPointPanel.border = empty()
    endPointPanel.isOpaque = false

    val routeInfo = EndpointSchema.getRouteInfo(affectedEndpoint.route)
    val routeCodeObjectId = affectedEndpoint.codeObjectId
    val shortRouteName =  routeInfo.shortName

    if (routeCodeObjectId != null && moreData.contains(routeCodeObjectId)) {
        val normalizedDisplayName = StringUtils.normalizeSpace(shortRouteName)
        val link = ActionLink(normalizedDisplayName) {
            openWorkspaceFileForSpan(project, moreData, routeCodeObjectId)
        }
        val targetClass = routeCodeObjectId.substringBeforeLast("\$_\$")

        link.toolTipText = asHtml("$targetClass: $shortRouteName")
        endPointPanel.add(link, BorderLayout.NORTH)

        val sampleTraceId = affectedEndpoint.sampleTraceId
        val traceSample = TraceSample(affectedEndpoint.name, sampleTraceId)
        val buttonToJaeger = buildButtonToJaeger(project, "Trace", affectedEndpoint.name, listOf(traceSample), InsightType.SpanScaling)
        if (buttonToJaeger != null) {
            endPointPanel.add(buttonToJaeger, BorderLayout.EAST)
        }
    } else {
        val line1 = JBLabel(asHtml("${affectedEndpoint.serviceName}: <b>$shortRouteName</b>"))
        endPointPanel.add(line1)
    }
    return endPointPanel
}

fun getRootCauseSpanPanel(project: Project, moreData: HashMap<String, Any>, rootCauseSpan: RootCauseSpan): JPanel {

    val rootCausePanel = JPanel(BorderLayout())
    rootCausePanel.border = empty()
    rootCausePanel.isOpaque = false

    val normalizedDisplayName = StringUtils.normalizeSpace(rootCauseSpan.displayName)
    val spanId = CodeObjectsUtil.createSpanId(rootCauseSpan.instrumentationLibrary, rootCauseSpan.name)

    if (moreData.contains(spanId)) {
        val link = ActionLink(normalizedDisplayName) {
            openWorkspaceFileForSpan(project, moreData, spanId)
        }
        link.toolTipText = asHtml(spanId)
        rootCausePanel.add(link,BorderLayout.CENTER)
    }else{
        val displayNameLabel = JBLabel(normalizedDisplayName, SwingConstants.TRAILING)
        displayNameLabel.toolTipText = asHtml(spanId)
        displayNameLabel.horizontalAlignment = SwingConstants.LEFT
        rootCausePanel.add(displayNameLabel,BorderLayout.CENTER)
    }

    val spanName = rootCauseSpan.name
    val sampleTraceId = rootCauseSpan.sampleTraceId
    val traceSample = TraceSample(spanName, sampleTraceId)
    val buttonToJaeger = buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample), InsightType.SpanScaling)
    if(buttonToJaeger != null) {
        rootCausePanel.add(buttonToJaeger, BorderLayout.EAST)
    }

    return rootCausePanel
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

private fun buildButtonToScalingGraph(project: Project, spanName: String, instLibrary: String, insightType: InsightType): JButton {
    val analyticsService = AnalyticsService.getInstance(project)
    val button = ListItemActionButton("Histogram")
    button.addActionListener {
        val htmlContent = analyticsService.getHtmlGraphForSpanScaling(instLibrary, spanName, Laf.Colors.PLUGIN_BACKGROUND.getHex())
        DigmaHTMLEditorProvider.openEditor(project, "Scaling Graph of Span $spanName", htmlContent)
        ActivityMonitor.getInstance(project).registerInsightButtonClicked("histogram", insightType)
    }

    return button
}