package org.digma.intellij.plugin.ui.list.insights

import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.HighUsageInsight
import org.digma.intellij.plugin.model.rest.insights.LowUsageInsight
import org.digma.intellij.plugin.model.rest.insights.NormalUsageInsight
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import javax.swing.Icon
import javax.swing.JPanel


private fun trafficUsagePanel(title: String, labelValue: String, countPerMinute: Int, icon: Icon, panelsLayoutHelper: PanelsLayoutHelper): JPanel {
    return createInsightPanel(title, asHtml(labelValue), icon, "${countPerMinute}/min",panelsLayoutHelper)
}

fun lowUsageInsightPanel(insight: LowUsageInsight, panelsLayoutHelper: PanelsLayoutHelper): JPanel {
    return trafficUsagePanel(
        "Endpoint low traffic", "Servicing a low number of requests",
        insight.maxCallsIn1Min, Icons.Insight.LOW_USAGE,panelsLayoutHelper
    )
}

fun normalUsageInsightPanel(insight: NormalUsageInsight, panelsLayoutHelper: PanelsLayoutHelper): JPanel {
    return trafficUsagePanel(
        "Endpoint normal level of traffic", "Servicing an average number of requests",
        insight.maxCallsIn1Min, Icons.Insight.NORMAL_USAGE,panelsLayoutHelper
    )
}

fun highUsageInsightPanel(insight: HighUsageInsight, panelsLayoutHelper: PanelsLayoutHelper): JPanel {
    return trafficUsagePanel(
        "Endpoint high traffic", "Servicing a high number of requests",
        insight.maxCallsIn1Min, Icons.Insight.HIGH_USAGE,panelsLayoutHelper
    )
}



