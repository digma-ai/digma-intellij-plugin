package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError
import org.digma.intellij.plugin.ui.model.insights.ErrorsListViewItem
import java.awt.Color
import javax.swing.JPanel

fun errorsPanel(listViewItem: ErrorsListViewItem):JPanel {


    val result = panel {

        val errorCount = listViewItem.insight.errorCount
        val unhandled = listViewItem.insight.unhandledCount
        val unexpected = listViewItem.insight.unexpectedCount

        listViewItem.insight.topErrors.forEach{

        }
        row("Errors") {}
            .comment("$errorCount errors($unhandled unhandled, $unexpected unexpected)")
        row {
            label("Error")
        }
        row {
            link("Error link", action = {

            })
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