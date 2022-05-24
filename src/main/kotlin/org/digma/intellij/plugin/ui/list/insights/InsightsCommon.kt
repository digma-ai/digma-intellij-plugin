package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.UIUtil
import org.digma.intellij.plugin.ui.common.iconPanelGrid
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.*

fun insightItemPanel(panel: JPanel): JPanel {
    val wrapper = JPanel()
    wrapper.border = empty(5)
    wrapper.layout = BorderLayout()
    wrapper.add(panel, BorderLayout.CENTER)
    return wrapper
}

fun insightGroupPanel(panel: JPanel): JPanel {

    panel.isOpaque = true
    panel.background = insightListBackground()
    panel.border = empty()
    val wrapper = JPanel()
    wrapper.layout = BorderLayout()
    wrapper.isOpaque = true
    wrapper.background = insightListBackground()
    wrapper.add(panel, BorderLayout.CENTER)
    wrapper.border = empty()
    return wrapper
}


fun insightListBackground(): Color {
    var background: Color = JBColor.namedColor("Editor.background", Color.BLACK)
    if (UIUtil.isUnderDarcula()) {
        background = Color(38, 38, 38)
    }
    return background
}

fun createInsightPanel(title: String, body: String, icon: Icon, iconText: String): JPanel {
    val title = panel {
        row {
            label(title)
                .bold()
                .verticalAlign(VerticalAlign.TOP)
        }
    }
    title.border = JBUI.Borders.empty(0)

    val iconPanel = iconPanelGrid(icon, iconText)
    iconPanel.border = JBUI.Borders.empty(0)

    val contentPanel = JBPanel<JBPanel<*>>()
    contentPanel.layout = BorderLayout(0, 0)

    val message = JLabel(body, SwingConstants.LEFT)

    contentPanel.add(title, BorderLayout.NORTH)
    contentPanel.add(message, BorderLayout.CENTER)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(contentPanel)
    result.add(Box.createHorizontalStrut(5))
    result.add(iconPanel)

    return insightItemPanel(result)
}