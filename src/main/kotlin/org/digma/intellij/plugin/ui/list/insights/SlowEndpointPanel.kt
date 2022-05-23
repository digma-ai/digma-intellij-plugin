package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.Duration
import org.digma.intellij.plugin.model.rest.insights.SlowEndpointInsight
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.iconPanelGrid
import java.awt.BorderLayout
import java.math.BigDecimal
import java.math.RoundingMode
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.SwingConstants


fun slowEndpointPanel(insight: SlowEndpointInsight): JBPanel<JBPanel<*>> {
    val title = panel {
        row {
            label("Slow Endpoint").bold()
        }
    }

    val descriptionNearIcon = evalDuration(insight.median)
    val iconPanel = iconPanelGrid(Icons.Insight.SLOW, descriptionNearIcon)
    iconPanel.border = JBUI.Borders.empty(10)

    val contentPanel = JBPanel<JBPanel<*>>()
    contentPanel.layout = BorderLayout()

    val message = JLabel(genContentAsHtml(insight), SwingConstants.LEFT)

    contentPanel.add(title, BorderLayout.NORTH)
    contentPanel.add(message, BorderLayout.CENTER)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(contentPanel)
    result.add(Box.createHorizontalStrut(20))
    result.add(iconPanel)
    result.toolTipText = genToolTip(insight)

    return result
}

fun genToolTip(insight: SlowEndpointInsight): String {
    return """
server processed 50% of requests in less than ${evalDuration(insight.endpointsMedian)}\n
server processed 25% of requests in higher than ${evalDuration(insight.endpointsP75)}
"""
}

fun evalDuration(duration: Duration): String {
    return "${duration.value}${duration.unit}"
}

fun genContentAsHtml(insight: SlowEndpointInsight): String {
    val pctVal = computePercentageDiff(insight)
    return asHtml("On average requests are slower than other endpoints by <span style=\"color:red\">${pctVal}")
}

fun computePercentageDiff(insight: SlowEndpointInsight): String {
    val decimal = computePercentageDiff(insight.median.raw, insight.endpointsMedianOfMedians.raw)
    return "${decimal.toPlainString()}%"
}

fun computePercentageDiff(value: Long, compare: Long): BigDecimal {
    val decimal = BigDecimal((value.toDouble() / compare.toDouble() - 1) * 100).setScale(0, RoundingMode.HALF_DOWN)
    return decimal
}
