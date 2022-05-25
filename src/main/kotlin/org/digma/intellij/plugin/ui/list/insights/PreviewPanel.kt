package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel


fun previewPanel(listViewItem: ListViewItem<CodeObjectSummary>): JPanel {

    val codeObjectSummary = listViewItem.modelObject

    val result = panel {
        row {
            link(codeObjectSummary.codeObjectId){
                //todo: action
            }
        }
    }

    result.background = insightListBackground()
    return result

}
