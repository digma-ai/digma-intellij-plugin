package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.HighUsageInsight
import org.digma.intellij.plugin.model.rest.insights.LowUsageInsight
import org.digma.intellij.plugin.model.rest.insights.NormalUsageInsight
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.asHtml
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel


private fun trafficUsagePanel(
        project: Project,
        insight: CodeObjectInsight,
        title: String,
        labelValue: String,
        countPerMinute: Int,
        iconsList: List<Icon>
): JPanel {
    val callsPerMinuteLabel = JLabel("${countPerMinute}/min")
    return createInsightPanel(
            project = project,
            insight = insight,
            title = title,
            description = asHtml(labelValue),
            iconsList = iconsList,
            bodyPanel = callsPerMinuteLabel,
            buttons = null,
            paginationComponent = null
    )
}

fun lowUsageInsightPanel(project: Project, insight: LowUsageInsight): JPanel {
    return trafficUsagePanel(
            project = project,
            insight = insight,
            title = "Endpoint low traffic",
            labelValue = "Servicing a low number of requests",
            countPerMinute = insight.maxCallsIn1Min,
            iconsList = listOf(Laf.Icons.Insight.LOW_USAGE)
    )
}

fun normalUsageInsightPanel(project: Project, insight: NormalUsageInsight): JPanel {
    return trafficUsagePanel(
            project = project,
            insight = insight,
            title = "Endpoint normal level of traffic",
            labelValue = "Servicing an average number of requests",
            countPerMinute = insight.maxCallsIn1Min,
            iconsList = listOf(Laf.Icons.Insight.NORMAL_USAGE)
    )
}

fun highUsageInsightPanel(project: Project, insight: HighUsageInsight): JPanel {
    return trafficUsagePanel(
            project = project,
            insight = insight,
            title = "Endpoint high traffic",
            labelValue = "Servicing a high number of requests",
            countPerMinute = insight.maxCallsIn1Min,
            iconsList = listOf(Laf.Icons.Insight.HIGH_USAGE)
    )
}



