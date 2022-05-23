package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import javax.swing.*

fun errorsPanel(project: Project, listViewItem: ListViewItem<ErrorInsight>):JPanel {

    val errorsPanel = panel {

        val errorCount = listViewItem.modelObject.errorCount
        val unhandled = listViewItem.modelObject.unhandledCount
        val unexpected = listViewItem.modelObject.unexpectedCount

        row {}
            .comment("Errors:  $errorCount errors($unhandled unhandled, $unexpected unexpected)").bold()
        row {
            panel {
                listViewItem.modelObject.topErrors.forEach{
                    row {
                        cell(topErrorPanel(it, listViewItem.modelObject))
                            .verticalAlign(VerticalAlign.FILL)
                            .horizontalAlign(HorizontalAlign.LEFT)
                    }.layout(RowLayout.INDEPENDENT)
                }
            }
        }
    }


    val expandLinkPanel = panel {
        row {
            link("Expand"){
                val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)
                actionListener.errorsExpandButtonClicked(listViewItem.modelObject)
            //action here
            }.horizontalAlign(HorizontalAlign.RIGHT)
        }.layout(RowLayout.INDEPENDENT)
    }
    expandLinkPanel.border = Borders.empty(10)


    val errorsWrapper = JBPanel<JBPanel<*>>()
    errorsWrapper.layout = BorderLayout()
    errorsWrapper.add(errorsPanel,BorderLayout.CENTER)
    errorsWrapper.border = BorderFactory.createEmptyBorder()

    val scrollPane = JBScrollPane()
    scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
    scrollPane.setViewportView(errorsWrapper)
    scrollPane.border = BorderFactory.createEmptyBorder()

    val expandPanel = JBPanel<JBPanel<*>>()
    expandPanel.layout = BorderLayout()
    expandPanel.add(expandLinkPanel,BorderLayout.SOUTH)


    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result,BoxLayout.X_AXIS)
    result.add(scrollPane)
    result.add(Box.createHorizontalStrut(20))
    result.add(expandPanel)

    return insightItemPanel(result)
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
                    label("From ${error.sourceCodeObjectId.split("\$_\$")[0]}")
                }
            }
        }
    }
}