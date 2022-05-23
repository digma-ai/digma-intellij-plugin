package org.digma.intellij.plugin.ui.common

import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridLayout
import javax.swing.*


fun panelOfUnsupported(caption: String): JPanel {
    return panel {
        row("Unsupported yet: '$caption'") {
        }.layout(RowLayout.PARENT_GRID)
    }
}

fun genericPanelForSingleInsight(listViewItem: ListViewItem<CodeObjectInsight>): JPanel {
    return panel {
        //temporary: need to implement logic
        row {
            label("Insight named '${listViewItem.modelObject.javaClass.simpleName}'")
            icon(Icons.TOOL_WINDOW)
        }
    }
}

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
