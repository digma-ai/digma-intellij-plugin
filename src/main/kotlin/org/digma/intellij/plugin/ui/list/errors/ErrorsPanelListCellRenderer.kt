package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel

class ErrorsPanelListCellRenderer : AbstractPanelListCellRenderer() {


    override fun createPanel(project: Project, value: ListViewItem<*>, index: Int): JPanel {
        return getOrCreatePanel(project, index, value as ListViewItem<CodeObjectError>)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreatePanel(project: Project, index: Int, value: ListViewItem<CodeObjectError>): JPanel {

        val model = value.modelObject

        val result = panel {
            //temporary: need to implement logic
            row {
                link(model.name) {
                    //error.codeObjectId
                    println("In action")
                }
                var from = "From me"
//                if (insight.codeObjectId != error.sourceCodeObjectId) {
//                    from = "From ${error.sourceCodeObjectId.split("\$_\$")[0]}"
//                }
                label(from)
                label("Score: ${model.scoreInfo.score}")
            }
        }

        return result
    }

}