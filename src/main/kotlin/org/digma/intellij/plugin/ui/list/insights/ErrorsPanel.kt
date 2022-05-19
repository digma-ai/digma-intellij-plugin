package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import javax.swing.JPanel

fun errorsPanel(listViewItem: ListViewItem<ErrorInsight>):JPanel {


    val result = panel {

        val errorCount = listViewItem.modelObject.errorCount
        val unhandled = listViewItem.modelObject.unhandledCount
        val unexpected = listViewItem.modelObject.unexpectedCount


        row("Errors") {}
            .comment("$errorCount errors($unhandled unhandled, $unexpected unexpected)")
        row {
            panel {
                listViewItem.modelObject.topErrors.forEach{
                    row{
//                        cell(topErrorsPanel(it))
                        label("An error:"+it.errorType)
                    }
                    row {
                        link("Error link", action = {

                        })
                    }
                }
            }

        }


    }

    result.isOpaque = true
//    result.background = Color.RED
    return result

}




fun topErrorsPanel(error: ErrorInsightNamedError,insight: ErrorInsight):JPanel{

    return panel {
        //temporary: need to implement logic
        row{
            link(error.errorType,{
                //todo: navigate to code
                //error.codeObjectId
            })
            if (insight.codeObjectId == error.sourceCodeObjectId){
                label("From me")
            }else{
                label("From ${error.sourceCodeObjectId}")
            }

        }
    }


}