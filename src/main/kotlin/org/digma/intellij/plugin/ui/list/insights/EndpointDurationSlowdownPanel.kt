package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.model.rest.insights.DurationSlowdownSource
import org.digma.intellij.plugin.model.rest.insights.EndpointDurationSlowdownInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import java.awt.BorderLayout
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

fun endpointDurationSlowdownPanel(
    project: Project,
    insight: EndpointDurationSlowdownInsight,
    panelsLayoutHelper: PanelsLayoutHelper
): JPanel {

    val rootPanel = createDefaultBoxLayoutYAxisPanel()

    val p50Sources = insight.durationSlowdownSources.filter { it.percentile == "0.5" }
    if (p50Sources.isNotEmpty()){
        addSlowdownSources(project, rootPanel, "Affecting most requests:", p50Sources, panelsLayoutHelper)
    }

    val p95Sources = insight.durationSlowdownSources.filter { it.percentile == "0.95" }
    if (p95Sources.isNotEmpty()){
        addSlowdownSources(project, rootPanel, "Affecting ~5% of requests:", p95Sources, panelsLayoutHelper)
    }

    val result = createInsightPanel(
        project = project,
        insight = insight,
        title = "Duration Slowdown Source Detected",
        description = "Found spans slowing the endpoint",
        iconsList = listOf(Laf.Icons.Insight.DURATION),
        bodyPanel = rootPanel,
        buttons = null,
        paginationComponent = null
    )
    result.toolTipText = asHtml(asHtml(insight.spanInfo.displayName))
    return result
}

fun addSlowdownSources(
    project: Project,
    rootPanel: JPanel,
    header: String,
    sources: List<DurationSlowdownSource>,
    panelsLayoutHelper: PanelsLayoutHelper) {

    val normalizedDisplayName = StringUtils.normalizeSpace(header)
    val jbLabel = JBLabel(normalizedDisplayName)
    jbLabel.horizontalAlignment = SwingConstants.LEFT
    jbLabel.horizontalTextPosition = SwingConstants.LEFT
    val labelWrapper = JPanel(BorderLayout())
    labelWrapper.add(jbLabel, BorderLayout.WEST)
    labelWrapper.isOpaque = false
    labelWrapper.border = JBUI.Borders.empty()
    rootPanel.add(labelWrapper)

    val gridLayout = GridBagLayout()
    val gridPanel = JPanel(gridLayout)
    gridPanel.isOpaque = false
    for((index, source) in sources.withIndex()){
        slowdownDurationRowPanel(project, source, panelsLayoutHelper, gridPanel, gridLayout, index)
    }
    rootPanel.add(gridPanel)
}