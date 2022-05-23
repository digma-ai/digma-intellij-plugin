package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.HighUsageInsight
import org.digma.intellij.plugin.model.rest.insights.LowUsageInsight
import org.digma.intellij.plugin.model.rest.insights.NormalUsageInsight
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.iconPanelGrid
import java.awt.BorderLayout
import javax.swing.*


private fun trafficUsagePanel(title: String, labelValue: String, countPerMinute: Int, icon: Icon): JPanel {

    val title = panel {
        row {
            label(title).bold()
        }
    }

    val iconPanel = iconPanelGrid(icon, "${countPerMinute}/min")
    iconPanel.border = Borders.empty(10)

    val contentPanel = JBPanel<JBPanel<*>>()
    contentPanel.layout = BorderLayout()

    val message = JLabel(asHtml(labelValue), SwingConstants.LEFT)

    contentPanel.add(title, BorderLayout.NORTH)
    contentPanel.add(message, BorderLayout.CENTER)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(contentPanel)
    result.add(Box.createHorizontalStrut(20))
    result.add(iconPanel)

    return insightItemPanel(result)
}

fun lowUsageInsightPanel(insight: LowUsageInsight): JPanel {
    return trafficUsagePanel(
        "Endpoint low traffic", "Servicing a low number of requests",
        insight.maxCallsIn1Min, Icons.Insight.LOW_USAGE
    )
}

fun normalUsageInsightPanel(insight: NormalUsageInsight): JPanel {
    return trafficUsagePanel(
        "Endpoint normal level of traffic", "Servicing an average number of requests",
        insight.maxCallsIn1Min, Icons.Insight.NORMAL_USAGE
    )
}

fun highUsageInsightPanel(insight: HighUsageInsight): JPanel {
    return trafficUsagePanel(
        "Endpoint high traffic", "Servicing a high number of requests",
        insight.maxCallsIn1Min, Icons.Insight.HIGH_USAGE
    )
}



