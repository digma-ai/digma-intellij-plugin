package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI.Borders.empty
import org.digma.intellij.plugin.ui.common.iconPanelGrid
import org.digma.intellij.plugin.ui.list.listGroupPanel
import org.digma.intellij.plugin.ui.list.listItemPanel
import java.awt.BorderLayout
import javax.swing.*

fun insightItemPanel(panel: JPanel): JPanel {
    return listItemPanel(panel)
}

fun insightGroupPanel(panel: JPanel): JPanel {
    return listGroupPanel(panel)
}


fun createInsightPanel(title: String, body: String, icon: Icon, iconText: String): JPanel {
    return createInsightPanel(title, body, icon, iconText, true)
}

fun createInsightPanel(title: String, body: String, icon: Icon, iconText: String, wrap: Boolean): JPanel {
    val titlePanel = panel {
        row {
            label(title)
                .bold()
                .verticalAlign(VerticalAlign.TOP)
        }
    }
    titlePanel.border = empty(0)

    val message = JLabel(body, SwingConstants.LEFT)

    val contentPanel = JBPanel<JBPanel<*>>()
    contentPanel.layout = BorderLayout(0, 0)
    contentPanel.border = empty()
    contentPanel.add(titlePanel, BorderLayout.NORTH)
    contentPanel.add(message, BorderLayout.CENTER)

    val iconPanel = iconPanelGrid(icon, iconText)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(contentPanel)
    result.add(Box.createHorizontalStrut(5))
    result.add(iconPanel)
    result.border = empty()


    return if (wrap) {
        insightItemPanel(result)
    } else {
        result
    }
}