package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel


fun emptyPanel(value: ListViewItem): JPanel {
    return panel {
        row("Not implemented:  $value") {
            label(value.codeObjectId)
        }
    }
}