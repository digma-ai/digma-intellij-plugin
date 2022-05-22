
package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
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
