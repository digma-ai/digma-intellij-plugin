package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.model.rest.insights.RootCauseSpan
import org.digma.intellij.plugin.model.rest.insights.SpanScalingInsight
import org.digma.intellij.plugin.ui.common.CopyableLabelHtmlWithForegroundColor
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.list.openWorkspaceFileForSpan
import org.digma.intellij.plugin.ui.model.TraceSample
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

fun spanScalingListViewItemsPanel(project: Project, insight: SpanScalingInsight, moreData: HashMap<String, Any>): JPanel {
    val scalingPanel = createDefaultBoxLayoutYAxisPanel()
    scalingPanel.add(getScalingDescriptionPanel(insight))
    scalingPanel.add(getScalingCalculationsPanel(insight))

    if (insight.rootCauseSpans.isNotEmpty()) {
        scalingPanel.add(getRootCauseSpansPanel(project,moreData,insight))
    }



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

fun getRootCauseSpansPanel(project: Project, moreData: HashMap<String, Any>, insight: SpanScalingInsight): JPanel {

    val rootCauseSpansPanel = createDefaultBoxLayoutYAxisPanel()

    val causedByLabel = JLabel("Caused By:")
    causedByLabel.horizontalAlignment = SwingConstants.LEFT
    val causedByPanel = JPanel(BorderLayout())
    causedByPanel.border = JBUI.Borders.empty()
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
    rootCausePanel.border = JBUI.Borders.empty()
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