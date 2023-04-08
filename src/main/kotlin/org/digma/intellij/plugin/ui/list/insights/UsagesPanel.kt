package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.editor.getCurrentPageNumberForInsight
import org.digma.intellij.plugin.editor.updateListOfEntriesToDisplay
import org.digma.intellij.plugin.model.rest.insights.SpanFlow
import org.digma.intellij.plugin.model.rest.insights.SpanUsagesInsight
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.openWorkspaceFileForSpan
import org.digma.intellij.plugin.ui.model.TraceSample
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import javax.swing.*

private const val RECORDS_PER_PAGE = 2

fun spanUsagesPanel(project: Project, insight: SpanUsagesInsight, moreData: HashMap<String, Any>): JPanel {

    val uniqueInsightId = insight.codeObjectId + insight.type
    val lastPageNum: Int
    var topUsagePanel: DigmaResettablePanel? = null
    val paginationPanel = JPanel()
    val flowsToDisplay = ArrayList<SpanFlow>()
    val flows = insight.flows
    val spanName = insight.span;
    lastPageNum = countNumberOfPages(flows.size, RECORDS_PER_PAGE)

    topUsagePanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildTopUsagePanel(
                    topUsagePanel!!,
                    flowsToDisplay,
                    project,
                    moreData,
                    spanName
            )
            rebuildPaginationPanel(paginationPanel, lastPageNum,
                    flows, topUsagePanel, flowsToDisplay, uniqueInsightId, RECORDS_PER_PAGE, project)
        }
    }

    updateListOfEntriesToDisplay(flows, flowsToDisplay, getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum), RECORDS_PER_PAGE, project)
    buildTopUsagePanel(topUsagePanel, flowsToDisplay, project, moreData, spanName)

    return createInsightPanel(
            project = project,
            insight = insight,
            title = "Top Usage",
            description = "",
            iconsList = null,
            bodyPanel = topUsagePanel,
            buttons = null,
            paginationComponent = buildPaginationRowPanel(lastPageNum, paginationPanel,
                    flows, topUsagePanel, flowsToDisplay, uniqueInsightId, RECORDS_PER_PAGE, project)
    )
}

private fun rebuildTopUsagePanel(
        topUsagePanel: DigmaResettablePanel,
        flowsToDisplay: List<SpanFlow>,
        project: Project,
        moreData: HashMap<String, Any>,
        spanName: String
) {
    topUsagePanel.removeAll()
    buildTopUsagePanel(topUsagePanel, flowsToDisplay, project, moreData, spanName)
}

private fun buildTopUsagePanel(
        topUsagePanel: DigmaResettablePanel,
        flowsToDisplay: List<SpanFlow>,
        project: Project,
        moreData: HashMap<String, Any>,
        spanName: String
) {
    topUsagePanel.layout = BoxLayout(topUsagePanel, BoxLayout.Y_AXIS)
    topUsagePanel.isOpaque = false

    flowsToDisplay.forEach { flow: SpanFlow ->
        topUsagePanel.add(getTopUsagePanel(project, moreData, flow, spanName))
    }
}

fun getTopUsagePanel(project: Project, moreData: HashMap<String, Any>,
                     spanFlow: SpanFlow, spanName: String): JPanel {

    val flowsListPanel = JBPanel<JBPanel<*>>()
    flowsListPanel.layout = BoxLayout(flowsListPanel, BoxLayout.Y_AXIS)
    flowsListPanel.border = JBUI.Borders.empty()
    flowsListPanel.isOpaque = false

    val line = createDefaultBoxLayoutLineAxisPanel()
    line.isOpaque = false

    val percentageLabel = CopyableLabelHtml(asHtml("${span(String.format("%.1f", spanFlow.percentage))}% "))
    percentageLabel.border = JBUI.Borders.emptyRight(5)

    line.add(percentageLabel, BorderLayout.WEST)

    var spanName = spanName // default, just in case first service is not found
    spanFlow.firstService?.let { firstService ->
        line.add(CopyableLabelHtml(asHtml(spanGrayed(firstService.service + ": "))))
        addSpanLinkIfPossible(project, firstService, moreData, line)
        spanName = firstService.span
    }
    spanFlow.intermediateSpan?.let { intermediateSpan ->
        line.add(CopyableLabelHtml(asHtml(" ${spanGrayed(Html.ARROW_RIGHT)} ${span(intermediateSpan)}")))
    }
    spanFlow.lastService?.let { lastService ->
        line.add(CopyableLabelHtml(asHtml(" ${spanGrayed(Html.ARROW_RIGHT)} ${spanGrayed(lastService.service + ": ")}")))
        addSpanLinkIfPossible(project, lastService, moreData, line)
    }
    spanFlow.lastServiceSpan?.let { lastServiceSpan ->
        line.add(CopyableLabelHtml(asHtml(" ${spanGrayed(Html.ARROW_RIGHT)} ${span(lastServiceSpan)}")))
    }
    var traceSample: TraceSample? = null
    spanFlow.sampleTraceIds.firstOrNull()?.let { sampleTraceId ->
        traceSample = TraceSample(spanName, sampleTraceId)
    }
    flowsListPanel.add(line)
    buildJPanelWithButtonToJaeger(flowsListPanel, traceSample, project, spanName)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.add(flowsListPanel, BorderLayout.CENTER)
    result.isOpaque = false

    return result;
}

fun addSpanLinkIfPossible(project: Project, service: SpanFlow.Service, moreData: HashMap<String, Any>, panel: JPanel) {
    if (moreData.contains(service.codeObjectId)){
        var spanName = service.span;
        val link = ActionLink(spanName) {
            openWorkspaceFileForSpan(project, moreData, service.codeObjectId)
        }
        var targetClass = service.codeObjectId.substringBeforeLast("\$_\$");

        link.toolTipText = asHtml("$targetClass: $spanName")
        panel.add(link)
    }
}