package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.ui.addKeyboardAction
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.EtchedBorder

fun errorsPanel(listViewItem: ListViewItem<ErrorInsight>):JPanel {

    val errorsPanel = panel {

        val errorCount = listViewItem.modelObject.errorCount
        val unhandled = listViewItem.modelObject.unhandledCount
        val unexpected = listViewItem.modelObject.unexpectedCount

        row("Errors") {}
            .comment("$errorCount errors($unhandled unhandled, $unexpected unexpected)")
        row {
            panel {
                listViewItem.modelObject.topErrors.forEach{
                    row {
                        cell(topErrorPanel(it, listViewItem.modelObject))
                            .verticalAlign(VerticalAlign.FILL)
                    }.layout(RowLayout.INDEPENDENT)
                }
            }
        }
    }


    val linkPanel = panel {
        row {
            link("Expand"){

            }.horizontalAlign(HorizontalAlign.CENTER)
        }.layout(RowLayout.INDEPENDENT)

    }



    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result,BoxLayout.X_AXIS)

    errorsPanel.minimumSize = Dimension(50,100)
    val errorsWrapper = JBPanel<JBPanel<*>>()
    errorsWrapper.layout = BorderLayout()
    errorsWrapper.add(errorsPanel,BorderLayout.CENTER)
    errorsWrapper.minimumSize = Dimension(50,100)
//    errorsWrapper.border = Borders.empty(10)

    val scrollPane = JBScrollPane()
    scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
    scrollPane.setViewportView(errorsWrapper)
//    scrollPane.setViewportView(errorsPanel)
    result.add(scrollPane)

    val expandPanel = JBPanel<JBPanel<*>>()
    expandPanel.layout = BorderLayout()
    expandPanel.add(linkPanel,BorderLayout.SOUTH)

    result.add(expandPanel)

    return result
}




fun topErrorPanel(error: ErrorInsightNamedError,insight: ErrorInsight):JPanel{

    return panel {
        //temporary: need to implement logic
        row {
            link(error.errorType) {
                //error.codeObjectId
                println("In action")
            }
            if (insight.codeObjectId == error.sourceCodeObjectId) {
                label("From me")
            }

        }
        if (insight.codeObjectId != error.sourceCodeObjectId) {
            indent {
                row {
                    label("From ${error.sourceCodeObjectId} Many major errors occur or propagate through this function.Many major errors occur or propagate through this function.")
                }
            }
        }
    }
}