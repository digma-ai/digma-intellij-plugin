package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.JBUI.Borders.emptyBottom
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.model.rest.insights.RootCauseSpan
import org.digma.intellij.plugin.model.rest.insights.SpanScalingInsight
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.openWorkspaceFileForSpan
import org.digma.intellij.plugin.ui.model.TraceSample
import java.awt.BorderLayout
import javax.swing.*


fun spanScalingListViewItemsPanel(project: Project, insight: SpanScalingInsight, moreData: HashMap<String, Any>): JPanel {
    val scalingPanel = createDefaultBoxLayoutYAxisPanel()
    scalingPanel.add(getScalingCalculationsPanel(insight))
    scalingPanel.border = emptyBottom(2)

    if (insight.rootCauseSpans.isNotEmpty()) {
        scalingPanel.add(getRootCauseSpansPanel(project,moreData,insight))
    }

    val buttonToGraph = buildButtonToScalingGraph(project, insight.spanName,insight.spanInstrumentationLibrary)

    val backwardsCompatibilityTitle = "Scaling Issue Found";
    val backwardsCompatibilityDescription = "Significant performance degradation at ${insight.turningPointConcurrency} executions/second";

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

    val causedByLabel = JLabel("Caused By:")
    causedByLabel.horizontalAlignment = SwingConstants.LEFT
    val causedByPanel = JPanel(BorderLayout())
    causedByPanel.border = empty()
    causedByPanel.isOpaque = false
    causedByPanel.add(causedByLabel,BorderLayout.WEST)
    rootCauseSpansPanel.add(causedByPanel)

    insight.rootCauseSpans.let { spans ->
        repeat(spans.size) {index ->
            rootCauseSpansPanel.add(getRootCauseSpanPanel(project,moreData,spans[index]))
        }
    }

    return rootCauseSpansPanel
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
    val buttonToJaeger = buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample))
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


private fun buildButtonToScalingGraph(project: Project, spanName: String, instLibrary: String): JButton {
    val analyticsService = AnalyticsService.getInstance(project)
    val button = ListItemActionButton("Histogram")
    button.addActionListener {
        val htmlContent = analyticsService.getHtmlGraphForSpanScaling(instLibrary, spanName, Laf.Colors.PLUGIN_BACKGROUND.getHex())
        HTMLEditorProvider.openEditor(project, "Scaling Graph of Span $spanName", htmlContent)
        ActivityMonitor.getInstance(project).registerInsightButtonClicked("scaling-histogram")
    }

    return button
}