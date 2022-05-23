package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.list.insights.insightItemPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridLayout
import javax.swing.*


fun iconPanel(icon: Icon, text: String): JPanel {
    val panel = JPanel()
    panel.layout = BorderLayout(5, 10)
    panel.add(JLabel(icon), BorderLayout.CENTER)
    val label = JLabel(text)
    label.horizontalAlignment = SwingConstants.CENTER
    panel.add(label, BorderLayout.SOUTH)
    panel.border = BorderFactory.createEmptyBorder()
    panel.background = Color.BLUE
    return panel
}

fun iconPanelBox(icon: Icon, text: String): JPanel {
    val panel = JPanel()
    panel.layout = BoxLayout(panel,BoxLayout.Y_AXIS)
    val iconLabel = JLabel(icon)
    panel.add(iconLabel)
    val label = JLabel(text)
    panel.add(label)
    panel.border = BorderFactory.createEmptyBorder()
    return panel
}

fun iconPanelGrid(icon: Icon, text: String): JPanel {
    val panel = JPanel()
    panel.layout = GridLayout(2,1)
    val iconLabel = JLabel(icon)
    panel.add(iconLabel)
    val label = JLabel(text)
    panel.add(label)
    panel.border = BorderFactory.createEmptyBorder()
    return panel
}



fun emptyPanel(value: ListViewItem<*>): JPanel {
    val result = panel {
        row("Not implemented:") {

        }.comment(value.javaClass.simpleName).bold()
        row{
            icon(AllIcons.General.Error)
                .horizontalAlign(HorizontalAlign.LEFT)
            label("value: ${value.modelObject?.javaClass}")
                .horizontalAlign(HorizontalAlign.FILL)
        }
    }

    return insightItemPanel(result)
}