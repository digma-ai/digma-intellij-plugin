package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.model.rest.insights.EndpointBreakdownComponent
import org.digma.intellij.plugin.model.rest.insights.EndpointBreakdownInsight
import org.digma.intellij.plugin.ui.Circle
import org.digma.intellij.plugin.ui.PieChart
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.ui.layouts.ResizableFlowLayout
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

    val colorPoolForUnknownTypes = mutableListOf<Color>(JBColor.PINK, JBColor.GREEN, JBColor.RED, JBColor.CYAN, JBColor.MAGENTA)

    val listPanel = JBPanel<JBPanel<*>>(ResizableFlowLayout(FlowLayout.LEFT, 10, 0))
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
        "Internal" -> Color.decode("0xCBCB41")
        "DB Queries" -> Color.decode("0xF55385")
        "HTTP Clients" -> Color.decode("0x519ABA")
        "Rendering" -> Color.decode("0xE37933")
        else -> null
    }
}