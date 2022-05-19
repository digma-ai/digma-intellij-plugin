package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel


fun emptyPanel(value: ListViewItem<*>): JPanel {
    return panel {
        row("Not implemented:") {

        }.layout(RowLayout.PARENT_GRID)
//        row("Not implemented: ${value.javaClass}") {
//            value.modelObject?.javaClass?.let { label(it.canonicalName) }
//        }
    }
}