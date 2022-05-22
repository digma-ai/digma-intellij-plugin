package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import javax.swing.*


fun iconPanel(icon: Icon, text: String): JPanel {
    val panel = JPanel()
    panel.layout = BorderLayout(5, 10)
    panel.add(JLabel(icon), BorderLayout.CENTER)
    val label = JLabel(text)
    label.horizontalAlignment = SwingConstants.CENTER
    panel.add(label, BorderLayout.SOUTH)
    panel.border = BorderFactory.createEmptyBorder()
    return panel
}



fun emptyPanel(value: ListViewItem<*>): JPanel {
    return panel {
        row("Not implemented:") {

        }.comment(value.javaClass.simpleName)
        row{
            icon(AllIcons.General.Error)
                .horizontalAlign(HorizontalAlign.LEFT)
            label("value: ${value.modelObject?.javaClass}")
                .horizontalAlign(HorizontalAlign.FILL)
        }
    }
}