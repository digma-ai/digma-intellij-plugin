package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.HighUsageInsight
import org.digma.intellij.plugin.model.rest.insights.LowUsageInsight
import org.digma.intellij.plugin.model.rest.insights.NormalUsageInsight
import javax.swing.Icon
import javax.swing.JPanel


private fun trafficUsagePanel(title: String, labelValue: String, icon: Icon): JPanel {
    val result = panel {
        row {
            label(title).bold()
        }
        row {
            label(labelValue)
        }
        row {
            icon(icon)
        }
    }

    return result
}

fun lowUsageInsightPanel(insight: LowUsageInsight): JPanel {
    return trafficUsagePanel("Endpoint low traffic", "Servicing a low number of requests", Icons.Insight.LOW_USAGE)
}

fun normalUsageInsightPanel(insight: NormalUsageInsight): JPanel {
    return trafficUsagePanel(
        "Endpoint normal level of traffic",
        "Servicing an average number of requests",
        Icons.Insight.NORMAL_USAGE
    )
}

fun highUsageInsightPanel(insight: HighUsageInsight): JPanel {
    return trafficUsagePanel("Endpoint high traffic", "Servicing a high number of requests", Icons.Insight.HIGH_USAGE)
}



