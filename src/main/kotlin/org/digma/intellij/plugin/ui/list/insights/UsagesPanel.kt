package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import org.digma.intellij.plugin.editor.getCurrentPageNumberForInsight
import org.digma.intellij.plugin.editor.updateListOfEntriesToDisplay
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.SpanFlow
import org.digma.intellij.plugin.model.rest.insights.SpanUsagesInsight
import org.digma.intellij.plugin.navigation.codeless.showInsightsForSpan
import org.digma.intellij.plugin.ui.common.CopyableLabelHtml
import org.digma.intellij.plugin.ui.common.Html
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.span
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel


private const val RECORDS_PER_PAGE = 4

fun spanUsagesPanel(project: Project, insight: SpanUsagesInsight): JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type
    val lastPageNum: Int
    var topUsagePanel: DigmaResettablePanel? = null
    val paginationPanel = JPanel()
    val flowsToDisplay = ArrayList<SpanFlow>()
    val flows = insight.flows
    val spanName = insight.span
    lastPageNum = countNumberOfPages(flows.size, RECORDS_PER_PAGE)

    topUsagePanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildTopUsagePanel(
                topUsagePanel!!,
                flowsToDisplay,
                project,
                spanName
            )
            rebuildPaginationPanel(
                paginationPanel, lastPageNum,
                flows, topUsagePanel, flowsToDisplay, uniqueInsightId, RECORDS_PER_PAGE, project, insight.type
            )
        }
    }

    updateListOfEntriesToDisplay(flows, flowsToDisplay, getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum), RECORDS_PER_PAGE, project)
    buildTopUsagePanel(topUsagePanel, flowsToDisplay, project, spanName)

    return createInsightPanel(
        project = project,
        insight = insight,
        title = "Top Usage",
        description = "",
        iconsList = null,
        bodyPanel = topUsagePanel,
        buttons = null,
        paginationComponent = buildPaginationRowPanel(
            lastPageNum, paginationPanel,
            flows, topUsagePanel, flowsToDisplay, uniqueInsightId, RECORDS_PER_PAGE, project, insight.type
        )
    )
}

private fun rebuildTopUsagePanel(
    topUsagePanel: DigmaResettablePanel,
    flowsToDisplay: List<SpanFlow>,
    project: Project,
    spanName: String,
) {
    topUsagePanel.removeAll()
    buildTopUsagePanel(topUsagePanel, flowsToDisplay, project, spanName)
}

private fun buildTopUsagePanel(
    topUsagePanel: DigmaResettablePanel,
    flowsToDisplay: List<SpanFlow>,
    project: Project,
    spanName: String,
) {
    topUsagePanel.layout = BoxLayout(topUsagePanel, BoxLayout.Y_AXIS)
    topUsagePanel.isOpaque = false

    flowsToDisplay.forEach { flow: SpanFlow ->
        topUsagePanel.add(getTopUsagePanel(project, flow, spanName))
        topUsagePanel.add(Box.createRigidArea(Dimension(0, 5)))
    }
}

fun getTopUsagePanel(
    project: Project,
    spanFlow: SpanFlow, origSpanName: String,
): JPanel {

    val flowPanel = JBPanel<JBPanel<*>>(BorderLayout())
    flowPanel.border = JBUI.Borders.empty()
    flowPanel.isOpaque = false

    val percentageLabel = CopyableLabelHtml(asHtml("${span(String.format("%.1f", spanFlow.percentage))}% "))
    percentageLabel.preferredSize = Dimension(Laf.scaleSize(50), 1)
    flowPanel.add(percentageLabel, BorderLayout.WEST)

    val line = JBPanel<JBPanel<*>>(WrapLayout(FlowLayout.LEFT, 5, 0))
    line.isOpaque = false
    var spanName = origSpanName // default, just in case first service is not found
    spanFlow.firstService?.let { firstService ->
        line.add(CopyableLabelHtml(asHtml(spanGrayed(firstService.service + ": "))))
        addSpanLinkIfPossible(project, firstService, line)
        spanName = firstService.span
    }
    spanFlow.intermediateSpan?.let { intermediateSpan ->
        line.add(CopyableLabelHtml(asHtml(" ${spanGrayed(Html.ARROW_RIGHT)} ${span(intermediateSpan)}")))
    }
    spanFlow.lastService?.let { lastService ->
        line.add(CopyableLabelHtml(asHtml(" ${spanGrayed(Html.ARROW_RIGHT)} ${spanGrayed(lastService.service + ": ")}")))
        addSpanLinkIfPossible(project, lastService, line)
    }
    spanFlow.lastServiceSpan?.let { lastServiceSpan ->
        line.add(CopyableLabelHtml(asHtml(" ${spanGrayed(Html.ARROW_RIGHT)} ${span(lastServiceSpan)}")))
    }
    var traceSample: TraceSample? = null
    spanFlow.sampleTraceIds.firstOrNull()?.let { sampleTraceId ->
        traceSample = TraceSample(spanName, sampleTraceId)
    }
    flowPanel.add(line, BorderLayout.CENTER)

    val buttonToJaeger = buildButtonToJaeger(project, "Trace", spanName, listOf(traceSample), InsightType.SpanUsages)
    if (buttonToJaeger != null) {
        val wrapper = JPanel(BorderLayout())
        wrapper.isOpaque = false
        wrapper.border = JBUI.Borders.emptyTop(5)
        wrapper.add(buttonToJaeger, BorderLayout.NORTH)
        flowPanel.add(wrapper, BorderLayout.EAST)
    }

    return flowPanel
}

fun addSpanLinkIfPossible(project: Project, service: SpanFlow.Service, panel: JPanel) {
    val spanName = service.span
    val link = ActionLink(spanName) {
        showInsightsForSpan(project, service.spanCodeObjectId)
    }
    link.toolTipText = asHtml(spanName)
    panel.add(link)
}
