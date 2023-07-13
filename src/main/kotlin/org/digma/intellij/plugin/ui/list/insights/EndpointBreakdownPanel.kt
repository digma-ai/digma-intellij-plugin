package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import org.digma.intellij.plugin.model.rest.insights.EndpointBreakdownComponent
import org.digma.intellij.plugin.model.rest.insights.EndpointBreakdownInsight
import org.digma.intellij.plugin.ui.Circle
import org.digma.intellij.plugin.ui.PieChart
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.common.spanGrayed
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel

fun endpointBreakdownPanel(
    project: Project,
    insight: EndpointBreakdownInsight,
): JPanel {

    val colorPoolForUnknownTypes = mutableListOf<Color>(
        Color(0x5378b4)/*blue*/,
        Color(0x8da760)/*green*/,
        Color(0xb9914e)/*orange*/,
        Color(0x9e4747)/*red*/
    )

    val listPanel = JBPanel<JBPanel<*>>(WrapLayout(FlowLayout.LEFT, 10, 0))
    listPanel.isOpaque = false

    val pie = PieChart()
    pie.preferredSize = Dimension(50, 50)

    for (component in insight.components.sortedByDescending { c -> c.fraction }) {
        var color = getColorForKnownType(component)
        if (color == null) {
            color = colorPoolForUnknownTypes.removeFirst()
        }

        val percentage = component.fraction * 100
        val fractionText = if (percentage < 1) "<1" else percentage.toInt().toString()

        val dot = Circle()
        dot.background = color
        dot.preferredSize = Dimension(5, 6)
        dot.border = JBUI.Borders.emptyTop(1)
        val item = JPanel()
        item.isOpaque = false
        item.add(dot)
        item.add(JLabel(asHtml(spanGrayed(component.type) + "  ${spanBold(fractionText)}%")))

        listPanel.add(item)
        pie.items.add(PieChart.Item(color, component.fraction))
    }

    val bodyPanel = JPanel(BorderLayout())
    bodyPanel.isOpaque = false
    bodyPanel.border = JBUI.Borders.empty(10, 5, 0, 0)
    bodyPanel.add(pie, BorderLayout.WEST)
    bodyPanel.add(listPanel, BorderLayout.CENTER)

    return createInsightPanel(
        project = project,
        insight = insight,
        title = "Request Breakdown",
        description = "",
        iconsList = null,
        bodyPanel = bodyPanel,
        buttons = null,
        paginationComponent = null
    )
}

fun getColorForKnownType(component: EndpointBreakdownComponent): Color? {
    return when (component.type) {
        "Internal" -> Color(0x53AEB4)
        "DB Queries" -> Color(0xB180D7)
        "HTTP Clients" -> Color(0x75BEFF)
        "Rendering" -> Color(0xF55385)
        else -> null
    }
}