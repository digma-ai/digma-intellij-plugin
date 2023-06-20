package org.digma.intellij.plugin.ui.list.summaries

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.home.HomeSwitcherService
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.discovery.CodeObjectInfo
import org.digma.intellij.plugin.model.rest.insights.SpanDurationChangeInsight
import org.digma.intellij.plugin.model.rest.insights.SpanDurationsPercentile
import org.digma.intellij.plugin.model.rest.insights.TopErrorFlowsInsight
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.common.CopyableLabelHtml
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.buildLinkTextWithGrayedAndDefaultLabelColorPart
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.list.RoundedPanel
import org.digma.intellij.plugin.ui.list.commonListItemPanel
import org.digma.intellij.plugin.ui.list.errors.contentOfFirstAndLast
import org.digma.intellij.plugin.ui.list.insights.genericPanelForSingleInsight
import org.digma.intellij.plugin.ui.list.insights.percentileRowPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import org.digma.intellij.plugin.ui.service.TabsHelper
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JLabel
import javax.swing.JPanel


class SummaryPanelListCellRenderer : AbstractPanelListCellRenderer() {

    override fun createPanel(project: Project,
                             value: ListViewItem<*>,
                             index: Int,
                             panelsLayoutHelper: PanelsLayoutHelper): JPanel {
        return when (val model = value.modelObject) {
            is SummaryTypeTitle -> buildTitle(model)
            is TopErrorFlowsInsight.Error -> commonListItemPanel(buildError(model, project))
            is SpanDurationChangeInsight.Change -> commonListItemPanel(buildSpanDuration(model,  panelsLayoutHelper, project))
            else -> genericPanelForSingleInsight(project, model)
        }
    }
}

private fun buildTitle(model: SummaryTypeTitle): JPanel {
    val text = when (model.type) {
        InsightType.TopErrorFlows -> "New and Trending Errors"
        InsightType.SpanDurationChange -> "Performance Changes"
        else -> "Unknown"
    }
    val label = JLabel(text)
    label.border = empty(0, 7, 5, 7)
    label.isOpaque = false

    val panel = JPanel(BorderLayout())
    panel.add(label, BorderLayout.CENTER)
    panel.isOpaque = false
    return panel
}

private fun buildError(model: TopErrorFlowsInsight.Error, project: Project): JPanel {
    val panel = JPanel(BorderLayout())

    // Linked title
    val relativeFrom = CodeObjectInfo.extractMethodName(model.sourceCodeObjectId)
    val linkText = buildLinkTextWithGrayedAndDefaultLabelColorPart(model.name, "from", relativeFrom)
    val link = ActionLink(asHtml(linkText)) {
        project.service<HomeSwitcherService>().switchToInsights()
        val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
        actionListener.showErrorDetails(model.uid)
    }
    link.border = JBUI.Borders.emptyBottom(10)

    // Characteristic
    val characteristic = getCharacteristic(model)

    val titleAndCharacteristic = JPanel(BorderLayout())
    titleAndCharacteristic.isOpaque = false
    titleAndCharacteristic.add(link, BorderLayout.CENTER)
    titleAndCharacteristic.add(characteristic, BorderLayout.EAST)

    val firstAndLastHtml = contentOfFirstAndLast(model.firstOccurenceTime, model.lastOccurenceTime)
    val firstAndLastTime = CopyableLabelHtml(asHtml(firstAndLastHtml))

    panel.add(titleAndCharacteristic, BorderLayout.CENTER)
    panel.add(firstAndLastTime, BorderLayout.SOUTH)
    return panel
}

private fun getCharacteristic(model: TopErrorFlowsInsight.Error): JPanel {
    val text = when (model.scoreMovingAvg.coerceAtLeast(model.scoreRecency).coerceAtLeast(model.scoreTrendSlope)) {
        model.scoreRecency -> "Recent"
        model.scoreMovingAvg -> "Frequent"
        model.scoreTrendSlope -> "Escalating"
        else -> ""
    }
    val label = JLabel(text)
    label.border = empty(1, 5, 3, 5)

    val box = RoundedPanel.wrap(label, 5)
    box.background = Laf.Colors.PLUGIN_BACKGROUND

    val wrapper = JPanel(BorderLayout())
    wrapper.isOpaque = false
    wrapper.add(box, BorderLayout.NORTH)
    return wrapper
}

private fun buildSpanDuration(value: SpanDurationChangeInsight.Change, panelsLayoutHelper: PanelsLayoutHelper, project: Project): JPanel {

    val spanId = value.span.spanCodeObjectId

    val title =
        ActionLink(asHtml(value.span.displayName)) {
            project.service<HomeSwitcherService>().switchToInsights()
            project.service<InsightsViewOrchestrator>().showInsightsForCodelessSpan(spanId)
            project.service<TabsHelper>().notifyTabChanged(0)
        }

    title.toolTipText = value.span.displayName
    title.border = JBUI.Borders.emptyBottom(5)

    val durationsListPanel = JBPanel<JBPanel<*>>()
    durationsListPanel.layout = GridLayout(value.percentiles.size, 1, 0, 2)
    durationsListPanel.isOpaque = false

    for (percentile in value.percentiles.sortedBy(SpanDurationsPercentile::percentile)) {
        val durationsPanel = percentileRowPanel(percentile, panelsLayoutHelper, ArrayList())
        durationsListPanel.add(durationsPanel)
    }

    val result = JBPanel<JBPanel<*>>()
    result.layout = BorderLayout()
    result.isOpaque = false
    result.add(title, BorderLayout.CENTER)
    result.add(durationsListPanel, BorderLayout.SOUTH)
    return result
}

